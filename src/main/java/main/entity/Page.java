package main.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Table(indexes = {
        @Index(columnList = "site_id"),
        @Index(columnList = "path")
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    public Page() {
    }

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return path;
    }
}