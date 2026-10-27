package com.gieokham.api.memo.domain;

import com.gieokham.api.category.domain.Category;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "memo")
@Getter
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoPriority priority = MemoPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private  LocalDateTime updatedAt;

    protected Memo(){
    }

    public Memo(String content, MemoPriority priority){
        this.content = content;
        this.priority = priority != null ? priority : MemoPriority.MEDIUM;
    }

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content, MemoPriority priority) {
        this.content = content;
        this.priority = priority;
    }

    public void updateCompletion(boolean completed) {
        if(completed){
            if (this.completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
        } else {
          this.completedAt = null;
        }

    }

    public void changeCategory(Category category) {
        this.category = category;
    }


}
