package com.supermartijn642.tesseract.integration;

import net.neoforged.fml.ModList;

/**
 * @author Max Henkel - Pipez
 */
public class MekanismUtils {

    private static Boolean isLoaded;

    public static boolean isMekanismInstalled(){
        if(isLoaded == null){
            isLoaded = ModList.get().isLoaded("mekanism");
        }
        return isLoaded;
    }
}
