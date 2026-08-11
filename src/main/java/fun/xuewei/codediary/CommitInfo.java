package fun.xuewei.codediary;

/**
 * 一条 Git 提交的简要信息，用于列表展示。
 */
public final class CommitInfo {
    private final String hash;
    private final String subject;
    private final long commitTimeMillis;
    private final GitRepositoryInfo repository;

    public CommitInfo(String hash, String subject, long commitTimeMillis) {
        this(hash, subject, commitTimeMillis, null);
    }

    public CommitInfo(String hash, String subject, long commitTimeMillis, GitRepositoryInfo repository) {
        this.hash = hash != null ? hash : "";
        this.subject = subject != null ? subject : "";
        this.commitTimeMillis = commitTimeMillis;
        this.repository = repository;
    }

    public String getHash() {
        return hash;
    }

    /** 短 hash，前 7 位 */
    public String getShortHash() {
        return hash.length() > 7 ? hash.substring(0, 7) : hash;
    }

    public String getSubject() {
        return subject;
    }

    public long getCommitTimeMillis() {
        return commitTimeMillis;
    }

    public GitRepositoryInfo getRepository() {
        return repository;
    }
}
