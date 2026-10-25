package com.repository.service.client;

import java.util.List;

// GitHub's tree JSON: a list of every path in one version of the repository
public record GithubTree(
        String sha,               // fingerprint of this tree
        boolean truncated,        // true if GitHub stops early and list is incomplete
        List<Entry> tree          // Every file and folder
) {

    // One item in the list: a file , a folder, or a submodule (commit)
    public record Entry(
            String path,     // path fo file
            String type,     // type:- blob(file), tree(folder), commit
            String sha,
            Long size        // size in bytes
    ) {}
}
