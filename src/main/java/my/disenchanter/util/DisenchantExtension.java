package my.disenchanter.util;

import java.util.Set;

public interface DisenchantExtension {
    Set<String> disenchanter$getSelected();
    void disenchanter$toggle(String enchantId);
    boolean disenchanter$isExtracting();
}
