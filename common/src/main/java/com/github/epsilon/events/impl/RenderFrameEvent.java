package com.github.epsilon.events.impl;

import net.minecraft.client.DeltaTracker;

public class RenderFrameEvent {

    public static class Pre extends RenderFrameEvent {

        private final DeltaTracker deltaTracker;

        public Pre(DeltaTracker deltaTracker) {
            this.deltaTracker = deltaTracker;
        }

        public DeltaTracker getDeltaTracker() {
            return deltaTracker;
        }

    }

    public static class Post extends RenderFrameEvent {
    }

}
