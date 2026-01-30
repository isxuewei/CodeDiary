package fun.xuewei.codediary;

/**
 * 一条 Git 提交的简要信息，用于列表展示。
 */
public final class CommitInfo {
    private final String hash;
    private final String subject;
    private final long commitTimeMillis;

    public CommitInfo(String hash, String subject, long commitTimeMillis) {
        this.hash = hash != null ? hash : "";
        this.subject = subject != null ? subject : "";
        this.commitTimeMillis = commitTimeMillis;
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
}
