package com.tenco.blog.purchase;

import com.tenco.blog.board.Board;
import com.tenco.blog.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * 구매 내역 엔티티
 *
 * User와 Board의 구매 관계를 표현 함
 *
 * 한 사람의 사용자는 여러 게시글을 구매할 수 있다.
 * 한 게시글은 여러 사용자에게 구매 될 수 있다.()
 * User : Board - 다대다 관게로 표현이 되기 때문에 중간 테이블 (Purchase) 생성이 되어야 한다.\
 *
 * Purchase : User --> @ManyToOne --> joinColumn 이름 지정
 * Purchase : Board --> @ManyToOne --> joinColumn 이름 지정
 */
@Data
@NoArgsConstructor
@Entity
@Table(name="purchase_tb",
       uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_board", columnNames = {"user_id","board_id"})
       })
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // 단방향 관게: "Purchase --> User"
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    // 단방향 관게: "Purchase --> Board"
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id",nullable = false)
    private Board board;

    // 구매 지불한 포인트
    private Integer price;

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Purchase(User user, Board board, Integer price) {
        this.user = user;
        this.board = board;
        this.price = price;

    }
}
