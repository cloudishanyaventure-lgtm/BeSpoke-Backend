package com.BeSpoke.service;

import com.BeSpoke.dto.PlatformOptionDto;
import com.BeSpoke.dto.PlatformOptionRequest;
import com.BeSpoke.entity.PlatformOption;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.PlatformOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every picklist the product offers — cities, styles, budget bands, the material
 * library's "how it works" steps. Seeded once from {@link PlatformOptionDefaults},
 * then owned by the platform admin at /admin/options.
 */
@Service
@Transactional(readOnly = true)
public class PlatformOptionService {

    /**
     * Lists whose shipped catalogue keeps growing, so a database seeded with an earlier
     * (shorter) version is topped up on boot rather than staying stuck at 14 cities.
     * ponytail: a city the admin deletes comes back on the next restart — deactivate it
     * instead. Drop the key from this set once the catalogue stops moving.
     */
    private static final java.util.Set<String> TOP_UP_LISTS = java.util.Set.of("CITY");

    private final PlatformOptionRepository repository;
    private final AuditService auditService;

    public PlatformOptionService(PlatformOptionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * The human label for a stored code — "APARTMENT" → "Apartment", "L10_25" → "₹10–25 L".
     * Emails read the same wording the pickers show, and an option the admin has since
     * renamed or deleted still degrades to something readable rather than a raw enum.
     */
    public String label(String listKey, String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return repository.findByListKeyAndValue(listKey, value)
                .map(PlatformOption::getLabel)
                .orElseGet(() -> humanize(value));
    }

    static String humanize(String value) {
        String lower = value.replace('_', ' ').toLowerCase(java.util.Locale.ROOT).trim();
        return lower.isEmpty() ? "—" : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Active entries, grouped by list — one call serves every picker on the site. */
    public Map<String, List<PlatformOptionDto>> publicLists() {
        return group(repository.findByActiveTrueOrderByListKeyAscSortOrderAsc(),
                PlatformOptionDto::publicView);
    }

    /** Everything including disabled entries, for the admin screen. */
    public Map<String, List<PlatformOptionDto>> adminLists() {
        Map<String, List<PlatformOptionDto>> lists =
                group(repository.findAllByOrderByListKeyAscSortOrderAsc(), PlatformOptionDto::from);
        // Keep a known list visible after its last entry is deleted, so it can be refilled.
        for (String key : PlatformOptionDefaults.LISTS.keySet()) {
            lists.computeIfAbsent(key, k -> new ArrayList<>());
        }
        return lists;
    }

    private Map<String, List<PlatformOptionDto>> group(
            List<PlatformOption> rows, java.util.function.Function<PlatformOption, PlatformOptionDto> map) {
        Map<String, List<PlatformOptionDto>> lists = new LinkedHashMap<>();
        for (PlatformOption option : rows) {
            lists.computeIfAbsent(option.getListKey(), k -> new ArrayList<>()).add(map.apply(option));
        }
        return lists;
    }

    @Transactional
    public PlatformOptionDto create(User actor, PlatformOptionRequest request) {
        String listKey = request.listKey().trim();
        String value = valueOf(request);
        repository.findByListKeyAndValue(listKey, value).ifPresent(existing -> {
            throw new BadRequestException("\"" + value + "\" is already in that list");
        });
        PlatformOption option = new PlatformOption(listKey, value, request.label().trim(),
                blankToNull(request.note()),
                request.sortOrder() != null ? request.sortOrder() : nextSortOrder(listKey));
        if (request.active() != null) {
            option.setActive(request.active());
        }
        option = repository.save(option);
        auditService.log(actor, null, "OPTION_CREATED",
                "Added \"" + option.getLabel() + "\" to " + listKey);
        return PlatformOptionDto.from(option);
    }

    @Transactional
    public PlatformOptionDto update(User actor, Long id, PlatformOptionRequest request) {
        PlatformOption option = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Option not found"));
        String value = valueOf(request);
        repository.findByListKeyAndValue(option.getListKey(), value)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("\"" + value + "\" is already in that list");
                });
        option.setValue(value);
        option.setLabel(request.label().trim());
        option.setNote(blankToNull(request.note()));
        if (request.sortOrder() != null) {
            option.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            option.setActive(request.active());
        }
        option = repository.save(option);
        auditService.log(actor, null, "OPTION_UPDATED",
                "Updated \"" + option.getLabel() + "\" in " + option.getListKey());
        return PlatformOptionDto.from(option);
    }

    @Transactional
    public void delete(User actor, Long id) {
        PlatformOption option = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Option not found"));
        repository.delete(option);
        auditService.log(actor, null, "OPTION_DELETED",
                "Removed \"" + option.getLabel() + "\" from " + option.getListKey());
    }

    /** Rewrites sortOrder to the given id order — the admin screen's move up/down. */
    @Transactional
    public List<PlatformOptionDto> reorder(User actor, String listKey, List<Long> orderedIds) {
        List<PlatformOption> rows = repository.findByListKeyOrderBySortOrderAsc(listKey);
        Map<Long, PlatformOption> byId = new LinkedHashMap<>();
        rows.forEach(o -> byId.put(o.getId(), o));
        int order = 0;
        for (Long id : orderedIds) {
            PlatformOption option = byId.remove(id);
            if (option == null) {
                throw new BadRequestException("Option " + id + " is not in " + listKey);
            }
            option.setSortOrder(order++);
        }
        // Anything the caller did not mention keeps its relative place at the end.
        for (PlatformOption leftover : byId.values()) {
            leftover.setSortOrder(order++);
        }
        List<PlatformOption> saved = repository.saveAll(rows);
        auditService.log(actor, null, "OPTION_REORDERED", "Reordered " + listKey);
        return saved.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(PlatformOptionDto::from).toList();
    }

    private int nextSortOrder(String listKey) {
        return repository.findByListKeyOrderBySortOrderAsc(listKey).stream()
                .mapToInt(PlatformOption::getSortOrder).max().orElse(-1) + 1;
    }

    private static String valueOf(PlatformOptionRequest request) {
        String value = request.value() == null || request.value().isBlank()
                ? request.label() : request.value();
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Fills any list that has never been populated. Per list, not per row: once an
     * admin owns a list, deleting an entry must not resurrect it on the next boot.
     */
    @Transactional
    public int seedDefaults() {
        int added = 0;
        for (Map.Entry<String, List<PlatformOption>> entry : PlatformOptionDefaults.LISTS.entrySet()) {
            if (repository.existsByListKey(entry.getKey())) {
                added += backfillNotes(entry.getValue());
                if (TOP_UP_LISTS.contains(entry.getKey())) {
                    added += addMissing(entry.getValue());
                }
                continue;
            }
            repository.saveAll(entry.getValue());
            added += entry.getValue().size();
        }
        return added;
    }

    /** Adds shipped entries the live list has never had, keeping the file's order. */
    private int addMissing(List<PlatformOption> defaults) {
        int next = nextSortOrder(defaults.get(0).getListKey());
        List<PlatformOption> missing = new ArrayList<>();
        for (PlatformOption fallback : defaults) {
            if (repository.findByListKeyAndValue(fallback.getListKey(), fallback.getValue()).isEmpty()) {
                missing.add(new PlatformOption(fallback.getListKey(), fallback.getValue(),
                        fallback.getLabel(), fallback.getNote(), next++));
            }
        }
        repository.saveAll(missing);
        return missing.size();
    }

    /**
     * Fills in a note the shipped defaults carry but the live row is missing — how city
     * coordinates reach a database seeded before they existed. Only ever writes over a
     * blank: a note the admin typed is theirs, and a row they deleted stays deleted.
     */
    private int backfillNotes(List<PlatformOption> defaults) {
        int filled = 0;
        for (PlatformOption fallback : defaults) {
            if (fallback.getNote() == null) {
                continue;
            }
            PlatformOption live = repository
                    .findByListKeyAndValue(fallback.getListKey(), fallback.getValue())
                    .orElse(null);
            if (live == null || live.getNote() != null) {
                continue;
            }
            live.setNote(fallback.getNote());
            repository.save(live);
            filled++;
        }
        return filled;
    }
}
