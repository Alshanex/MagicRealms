package net.alshanex.magic_realms.entity;

import java.util.Collections;
import java.util.List;

public interface IExclusiveMercenary {
    String getExclusiveMercenaryName();
    String getExclusiveMercenaryPresentationMessage();

    default List<String> getExclusiveSpeechTranslationKeys() {
        return Collections.emptyList();
    }
}
