package histori.wiki;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor @AllArgsConstructor @ToString(of="title") @Slf4j
public class WikiArticle {

    @Getter @Setter private String title;
    @Getter @Setter private String text = "";

    public void addText(String line) { text += line; }

    public ParsedWikiArticle parse () { return new ParsedWikiArticle(title, WikiNode.parse(text)); }

    @JsonIgnore public boolean isRedirect() { return text.toLowerCase().trim().startsWith("#redirect"); }

}
