package com.dissayakesuper.web_pos_backend.reorder.repository;

import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReorderItemRepository extends JpaRepository<ReorderItem, Long> {

    List<ReorderItem> findAllByReorderId(Long reorderId);

    void deleteAllByReorderId(Long reorderId);
}
