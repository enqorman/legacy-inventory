package io.github.enqorman.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider {
    @Shadow @Final private RecipeBookWidget recipeBook;

    @Shadow private float mouseX;

    @Shadow private float mouseY;

    @Shadow private boolean narrow;

    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Unique private static boolean has_moved_slots = false;


    /**
     * @author
     * @reason
     */
    @Override
    public void init() {
        PlayerEntity player = this.handler.owner;

        assert this.client != null;
        assert this.client.interactionManager != null;

        if (this.client.interactionManager.hasCreativeInventory()) {
            this.client.setScreen(new CreativeInventoryScreen(player, ((ClientPlayerEntity)player).networkHandler.getEnabledFeatures(), (Boolean)this.client.options.getOperatorItemsTab().getValue()));
        } else {
            int OFFSET_X = -10;
            int OFFSET_Y = 8;

            {
                this.titleX += OFFSET_X;
                this.titleY += OFFSET_Y;
                // todo: move "Crafting" text
            }

            if (!InventoryScreenMixin.has_moved_slots) {
                int s = PlayerScreenHandler.CRAFTING_INPUT_START;
                int e = PlayerScreenHandler.CRAFTING_INPUT_END;

                for (int i = s; i < e; ++i) {
                    Slot slot = this.handler.slots.get(i);
                    this.handler.slots.set(i, new Slot(this.handler.getCraftingInput(), i, slot.x + OFFSET_X, slot.y + OFFSET_Y));
                }

                {
                    CraftingResultSlot slot = (CraftingResultSlot) this.handler.slots.get(PlayerScreenHandler.CRAFTING_RESULT_ID);
                    this.handler.slots.set(PlayerScreenHandler.CRAFTING_RESULT_ID, new CraftingResultSlot(player, this.handler.getCraftingInput(), this.handler.craftingResult, PlayerScreenHandler.CRAFTING_RESULT_ID, slot.x + OFFSET_X, slot.y + OFFSET_Y));
                }

                {
                    Slot slot = this.handler.slots.get(PlayerScreenHandler.OFFHAND_ID);
                    this.handler.slots.set(PlayerScreenHandler.OFFHAND_ID, new Slot(player.getInventory(), PlayerScreenHandler.OFFHAND_ID, slot.x + 75, slot.y) {
                        public void setStack(ItemStack stack) {
                            EquipmentSlot slot = EquipmentSlot.OFFHAND;
                            ItemStack currentStack = this.getStack();
                            Equipment equipment = Equipment.fromStack(stack);
                            if (equipment != null)
                                player.onEquipStack(slot, currentStack, stack);
                            super.setStack(stack);
                        }

                        public Pair<Identifier, Identifier> getBackgroundSprite() {
                            return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT);
                        }
                    });
                }

                InventoryScreenMixin.has_moved_slots = true;
            }

            super.init();

//            this.narrow = this.width < 379;
            this.narrow = false;
            this.recipeBook.initialize(this.width, this.height, this.client, this.narrow, this.handler);

//            this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
//            this.addDrawableChild(new TexturedButtonWidget(this.x + 104, this.height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, (button) -> {
//                this.recipeBook.toggleOpen();
//                this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
//                button.setPosition(this.x + 104, this.height / 2 - 22);
//                this.mouseDown = true;
//            }));
//            this.addSelectableChild(this.recipeBook);
//            this.setInitialFocus(this.recipeBook);
        }
    }

    /**
     * @author enqorman
     * @reason idk
     */
    @Overwrite
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        if (this.recipeBook.isOpen() && this.narrow) {
            this.drawBackground(context, delta, mouseX, mouseY);
            this.recipeBook.render(context, mouseX, mouseY, delta);
        } else {
            this.recipeBook.render(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            this.recipeBook.drawGhostSlots(context, this.x, this.y, false, delta);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
        this.recipeBook.drawTooltip(context, this.x, this.y, mouseX, mouseY);
        this.mouseX = (float)mouseX;
        this.mouseY = (float)mouseY;
    }
}