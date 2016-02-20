package histori.model.cache;

import histori.model.Vote;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.ExpirableBase;

public class VoteSummary extends ExpirableBase {

    public VoteSummary(String uuid) { setUuid(uuid); }

    @Override public Long getExpirationSeconds() { return null; }

    @Getter @Setter private long tally;
    @Getter @Setter private long count;
    @Getter @Setter private long upVotes;
    @Getter @Setter private long downVotes;

    public void tally(Vote vote) {
        tally += vote.getVote();
        count++;
        if (vote.getVote() > 0) upVotes++;
        if (vote.getVote() < 0) downVotes++;
    }
}