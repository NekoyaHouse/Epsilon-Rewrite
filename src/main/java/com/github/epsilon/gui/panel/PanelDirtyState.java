package com.github.epsilon.gui.panel;

public class PanelDirtyState {

    private boolean chromeDirty = true;
    private boolean railDirty = true;
    private boolean moduleListDirty = true;
    private boolean detailDirty = true;

    public void markAllDirty() {
        chromeDirty = true;
        railDirty = true;
        moduleListDirty = true;
        detailDirty = true;
    }

    public void markLayoutDirty() {
        chromeDirty = true;
        railDirty = true;
        moduleListDirty = true;
        detailDirty = true;
    }

    public void markRailDirty() {
        railDirty = true;
    }

    public void markModuleListDirty() {
        moduleListDirty = true;
    }

    public void markDetailDirty() {
        detailDirty = true;
    }

    public boolean consumeChromeDirty() {
        return consume(() -> chromeDirty, () -> chromeDirty = false);
    }

    public boolean consumeRailDirty() {
        return consume(() -> railDirty, () -> railDirty = false);
    }

    public boolean consumeModuleListDirty() {
        return consume(() -> moduleListDirty, () -> moduleListDirty = false);
    }

    public boolean consumeDetailDirty() {
        return consume(() -> detailDirty, () -> detailDirty = false);
    }

    private boolean consume(BooleanSupplier getter, Runnable clearAction) {
        if (!getter.getAsBoolean()) {
            return false;
        }
        clearAction.run();
        return true;
    }

    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}

