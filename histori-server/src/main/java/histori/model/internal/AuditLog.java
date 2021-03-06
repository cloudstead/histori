package histori.model.internal;

import histori.model.auth.RegistrationRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

@Entity @Accessors(chain=true) @NoArgsConstructor
public class AuditLog extends StrongIdentifiableBase {

    public AuditLog(LoginRequest login, String context, String notes) {
        setName(login.hasName() ? login.getName() : "-empty-");
        setContext(context);
        setUserAgent(login.getUserAgent());
        setNotes(notes);
    }

    public AuditLog(RegistrationRequest reg, String context, String notes) {
        setName(reg.hasName() ? reg.getName() : "-empty-");
        setContext(context);
        setUserAgent(reg.getUserAgent());
        setNotes(notes);
    }

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String name;

    @Column(length=200, nullable=false, updatable=false)
    @Getter @Setter private String context;

    @Column(length=1000, nullable=false, updatable=false)
    @Getter @Setter private String userAgent;

    @Column(length=16000, nullable=false, updatable=false)
    @Getter @Setter private String notes;
}
