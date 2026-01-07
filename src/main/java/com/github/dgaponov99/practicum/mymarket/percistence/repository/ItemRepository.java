package com.github.dgaponov99.practicum.mymarket.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
            select i
            from Item i
            where :searchText is null
                or lower(i.title) like lower(concat('%', cast(:searchText as string), '%'))
                or lower(i.description) like lower(concat('%', cast(:searchText as string), '%'))
            """)
    Page<Item> search(@Param("searchText") String searchText, Pageable pageable);

}
