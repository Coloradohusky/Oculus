package net.coderbot.batchedentityrendering.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.coderbot.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.BufferBuilder;

public class SegmentedBufferBuilder implements MemoryTrackingBuffer {
    private final BufferBuilder buffer;
    private final List<CustomRenderType> usedTypes;
    private CustomRenderType currentType;

    public SegmentedBufferBuilder() {
        // 2 MB initial allocation
        this.buffer = new BufferBuilder(512 * 1024);
        this.usedTypes = new ArrayList<>(256);

        this.currentType = null;
    }

    @Override
    public VertexConsumer getBuffer(CustomRenderType renderType) {
        if (!Objects.equals(currentType, renderType)) {
            if (currentType != null) {
                if (shouldSortOnUpload(currentType)) {
                    buffer.sortQuads(0, 0, 0);
                }

                buffer.end();
                usedTypes.add(currentType);
            }

            buffer.begin(renderType.mode(), renderType.format());

            currentType = renderType;
        }

        // Use duplicate vertices to break up triangle strips
        // https://developer.apple.com/library/archive/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/Art/degenerate_triangle_strip_2x.png
        // This works by generating zero-area triangles that don't end up getting rendered.
        // TODO: How do we handle DEBUG_LINE_STRIP?
        if (RenderTypeUtil.isTriangleStripDrawMode(currentType)) {
            ((BufferBuilderExt) buffer).splitStrip();
        }

        return buffer;
    }

    public List<BufferSegment> getSegments() {
        if (currentType == null) {
            return Collections.emptyList();
        }

        usedTypes.add(currentType);

        if (shouldSortOnUpload(currentType)) {
            buffer.sortQuads(0, 0, 0);
        }

        buffer.end();
        currentType = null;

        List<BufferSegment> segments = new ArrayList<>(usedTypes.size());

        for (CustomRenderType type : usedTypes) {
            Pair<BufferBuilder.State, ByteBuffer> pair = buffer.popNextBuffer();

            BufferBuilder.State drawState = pair.getFirst();
            ByteBuffer slice = pair.getSecond();

            segments.add(new BufferSegment(slice, drawState, type));
        }

        usedTypes.clear();

        return segments;
    }

    private static boolean shouldSortOnUpload(CustomRenderType type) {
        return ((RenderTypeAccessor) type).shouldSortOnUpload();
    }

    @Override
    public int getAllocatedSize() {
        return ((MemoryTrackingBuffer) buffer).getAllocatedSize();
    }

    @Override
    public int getUsedSize() {
        return ((MemoryTrackingBuffer) buffer).getUsedSize();
    }
}
