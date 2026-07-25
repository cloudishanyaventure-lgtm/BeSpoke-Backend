package com.BeSpoke.repository;

import com.BeSpoke.entity.RoomCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomCatalogItemRepository extends JpaRepository<RoomCatalogItem, Long> {

    List<RoomCatalogItem> findAllByOrderBySortOrderAsc();
}
