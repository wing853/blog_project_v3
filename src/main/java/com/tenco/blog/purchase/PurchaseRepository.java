package com.tenco.blog.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    // 기본적인 CRUD 생성 및 추가 기능 자동 생성 됨

    @Query("""
            SELECT p   FROM Purchase p WHERE p.user.id = : userId AND p.board.id = :boardId
            """)
    Optional<Purchase> findByUserIdAndBoardId(@Param("userId") Integer userId,
                                              @Param("boardId") Integer boardId);


    // 사용자(상세보기)와 게시글의 구매 여부 확인
    @Query("""
            SELECT COUNT(p) > 0 FROM Purchase p WHERE p.user.id = :userId AND p.board.id = :boardId
            """)
    boolean existsByUserIdAndBoardId(@Param("userId") Integer userId,
                                     @Param("boardId") Integer boardId);
}
