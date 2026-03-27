package com.github.epsilon.graphics.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ElementStore<T> {

    private final List<T> elements = new ArrayList<>();
    private boolean dirty = true;

    public void clear() {
        if (!elements.isEmpty()) {
            elements.clear();
            dirty = true;
        }
    }

    public void add(T element) {
        elements.add(element);
        dirty = true;
    }

    public void addAll(Collection<? extends T> values) {
        if (values.isEmpty()) {
            return;
        }
        elements.addAll(values);
        dirty = true;
    }

    public List<T> view() {
        return Collections.unmodifiableList(elements);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void markClean() {
        dirty = false;
    }
}

