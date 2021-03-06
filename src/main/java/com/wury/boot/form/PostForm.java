package com.wury.boot.form;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by WURI on 20/04/2016.
 */
public class PostForm {

    @NotEmpty
    private String postTitle = "";
    @NotEmpty
    private String postContent = "";

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }
}
