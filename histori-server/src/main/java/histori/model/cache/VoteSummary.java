package histori.model.cache;

import histori.model.Vote;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ExpirableBase;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable @NoArgsConstructor @Accessors(chain=true)
public class VoteSummary extends ExpirableBase {

    public VoteSummary(String uuid) { setUuid(uuid); }

    @Override public Long getExpirationSeconds() { return null; }

    @Column(nullable=true) @Getter @Setter private long tally;
    @Column(nullable=true) @Getter @Setter private long voteCount;
    @Column(nullable=true) @Getter @Setter private long upVotes;
    @Column(nullable=true) @Getter @Setter private long downVotes;

    public void tally(Vote vote) {
        tally += vote.getVote();
        voteCount++;
        if (vote.getVote() > 0) upVotes++;
        if (vote.getVote() < 0) downVotes++;
    }
}
