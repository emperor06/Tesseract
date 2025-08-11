package com.supermartijn642.tesseract.capabilities;

import java.util.ArrayList;
import java.util.List;

import com.supermartijn642.tesseract.EnumChannelType;
import com.supermartijn642.tesseract.TesseractBlockEntity;
import com.supermartijn642.tesseract.manager.Channel;
import com.supermartijn642.tesseract.manager.TesseractReference;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Created 3/20/2020 by SuperMartijn642
 */
public class CombinedEnergyStorage implements IEnergyStorage {

    private final Channel channel;
    private final TesseractBlockEntity requester;

    public CombinedEnergyStorage(Channel channel, TesseractBlockEntity requester){
        this.channel = channel;
        this.requester = requester;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate){
        if(this.pushRecurrentCall())
            return 0;

        if(!this.requester.canSend(EnumChannelType.ENERGY) || amount <= 0){
            this.popRecurrentCall();
            return 0;
        }

        int inserted = 0;
        int needed;
        final List<Distribution> receivers = new ArrayList<Distribution>();

        // Find all potential receivers and what they need
        for(TesseractReference location : this.channel.receivingTesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IEnergyStorage handler : entity.getSurroundingEnergyCapabilities()){
                        if(handler.canReceive() && (needed = handler.receiveEnergy(amount, true)) > 0)
                            receivers.add(new Distribution(handler, needed));
                    }
                }
            }
        }

        // do the maths
        Distribution.distributeFair(receivers, amount);

        // distribute everyone its fair share
        for(Distribution d : receivers){
            IEnergyStorage handler = (IEnergyStorage) d.handler;
            inserted += handler.receiveEnergy((int) d.given, simulate);
        }

        // some mods (Mekanism) may refuse a single FE so there may be some resources left to distribute
        int n = receivers.size();
        while(n --> 0 && inserted < amount){
            Distribution d = receivers.get(n);
            IEnergyStorage handler = (IEnergyStorage) d.handler;
            inserted += handler.receiveEnergy(amount - inserted, simulate);
        }

        this.popRecurrentCall();

        return inserted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate){
        if(this.pushRecurrentCall())
            return 0;

        if(!this.requester.canReceive(EnumChannelType.ENERGY) || maxExtract <= 0){
            this.popRecurrentCall();
            return 0;
        }

        int amount = maxExtract;

        loop:
        for(TesseractReference location : this.channel.sendingTesseracts){
            if(location.canBeAccessed() && location.canSend(EnumChannelType.ENERGY)){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IEnergyStorage storage : entity.getSurroundingEnergyCapabilities()){
                        if(!storage.canExtract())
                            continue;
                        int extracted = storage.extractEnergy(amount, simulate);
                        if(extracted > 0){
                            amount -= extracted;
                            if(amount <= 0)
                                break loop;
                        }
                    }
                }
            }
        }

        this.popRecurrentCall();

        return Math.max(0, maxExtract - amount);
    }

    @Override
    public int getEnergyStored(){
        if(this.pushRecurrentCall())
            return 0;

        int amount = 0;
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IEnergyStorage storage : entity.getSurroundingEnergyCapabilities())
                        amount += storage.getEnergyStored();
                }
            }
        }

        this.popRecurrentCall();

        return amount;
    }

    @Override
    public int getMaxEnergyStored(){
        if(this.pushRecurrentCall())
            return 0;

        int amount = 0;
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IEnergyStorage storage : entity.getSurroundingEnergyCapabilities())
                        amount += storage.getMaxEnergyStored();
                }
            }
        }

        this.popRecurrentCall();

        return amount;
    }

    @Override
    public boolean canExtract(){
        return this.requester.canReceive(EnumChannelType.ENERGY);
    }

    @Override
    public boolean canReceive(){
        return this.requester.canSend(EnumChannelType.ENERGY);
    }

    /**
     * Checks whether this is a recurrent call to this combined capability.
     * If not, it will just increase the recurrent call counter.
     */
    private boolean pushRecurrentCall(){
        if(this.requester.recurrentCalls >= 1)
            return true;
        this.requester.recurrentCalls++;
        return false;
    }

    private void popRecurrentCall(){
        this.requester.recurrentCalls--;
    }
}
