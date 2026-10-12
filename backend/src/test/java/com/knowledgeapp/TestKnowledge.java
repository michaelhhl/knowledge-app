package com.knowledgeapp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Temporary knowledge root used by integration tests; never touches the real knowledge/. */
public final class TestKnowledge {

    private TestKnowledge() {
    }

    public static Path create() {
        try {
            Path base = Files.createTempDirectory("knowledge-app-test");
            Path root = Files.createDirectories(base.resolve("knowledge"));
            Files.writeString(root.resolve("_index.md"), """
                    # 知识目录

                    - [请假流程](./leave-policy.md)
                    - [草稿篇](./secret-draft.md)
                    - [越界](../outside.md)
                    """);
            Files.writeString(root.resolve("leave-policy.md"), """
                    ---
                    id: kb-leave-policy
                    title: 请假流程
                    tags: [hr]
                    status: published
                    ---

                    # 请假流程

                    ## 年假申请
                    年假需要提前三个工作日在审批系统提交申请，由直属主管审批。

                    ## 病假
                    病假当天通知主管，返岗后补交医院证明。
                    """);
            Files.createDirectories(root.resolve("it"));
            Files.writeString(root.resolve("it/vpn.md"), """
                    ---
                    id: kb-vpn
                    title: VPN 连接
                    ---

                    # VPN 连接

                    ## 安装客户端
                    从内部软件中心安装 VPN 客户端，使用工号登录。
                    """);
            Files.writeString(root.resolve("secret-draft.md"), """
                    ---
                    id: kb-secret
                    title: 草稿篇
                    status: draft
                    ---

                    # 草稿篇

                    ## 未公开
                    紫色独角兽项目的预算是九百万。
                    """);
            Files.writeString(base.resolve("outside.md"), "# outside\n\n## 机密\n不应被读取。\n");
            return root;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
