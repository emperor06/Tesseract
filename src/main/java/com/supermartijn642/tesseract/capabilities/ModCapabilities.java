package com.supermartijn642.tesseract.capabilities;

import org.jetbrains.annotations.Nullable;

import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * @author Max Henkel - Pipez
 */
public class ModCapabilities {

    public static final BlockCapability<IChemicalHandler, @Nullable Direction> CHEMICAL_HANDLER_CAPABILITY = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);
    public static final ItemCapability<IChemicalHandler, Void> CHEMICAL_HANDLER_ITEM_CAPABILITY = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

}
