/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.OwnershipTrackedBridge;
import org.spongepowered.common.bridge.world.chunk.ActiveChunkReferantBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.entity.PlayerTracker;

import java.util.Optional;

@Mixin(value = BlockRailBase.class, remap = false)
public class BlockRailBaseMixin_Forge {

    // Used to transfer tracking information from minecarts to block positions
    @Inject(method = "onMinecartPass", at = @At(value = "HEAD"))
    private void onMinecartRailPass(final World world, final net.minecraft.entity.item.EntityMinecart cart, final BlockPos pos, final CallbackInfo ci) {
        if (!(cart instanceof OwnershipTrackedBridge)) {
            return;
        }
        final OwnershipTrackedBridge ownerBridge = (OwnershipTrackedBridge) cart;
        final Optional<User> notifier = ownerBridge.tracked$getNotifierReference();
        final Optional<User> owner = ownerBridge.tracked$getOwnerReference();
        if (owner.isPresent() || notifier.isPresent()) {
            final Chunk chunk = (Chunk) ((ActiveChunkReferantBridge) cart).bridge$getActiveChunk();
            final boolean useActiveChunk = chunk != null && chunk.x == pos.getX() >> 4 && chunk.z == pos.getZ() >> 4;
            final ChunkBridge spongeChunk = (ChunkBridge) (useActiveChunk ? chunk : world.getChunk(pos));
            final Block block = ((Chunk) spongeChunk).getBlockState(pos).getBlock();
            if (notifier.isPresent()) {
                spongeChunk.addTrackedBlockPosition(block, pos, notifier.get(), PlayerTracker.Type.NOTIFIER);
            } else {
                owner.ifPresent(
                    user -> spongeChunk.addTrackedBlockPosition(block, pos, user, PlayerTracker.Type.NOTIFIER));
            }
        }
    }
}
