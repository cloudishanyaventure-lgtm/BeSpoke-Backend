package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByLeadOrderByVersionDesc(Lead lead);

    Optional<Quote> findFirstByLeadOrderByVersionDesc(Lead lead);

    boolean existsByLeadAndStatusIn(Lead lead, Collection<QuoteStatus> statuses);

    List<Quote> findByLeadAndStatusInOrderByVersionDesc(Lead lead, Collection<QuoteStatus> statuses);

    List<Quote> findAllByOrderByCreatedAtDesc();

    List<Quote> findByStatusOrderByCreatedAtDesc(QuoteStatus status);
}
