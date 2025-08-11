package com.supermartijn642.tesseract.capabilities;

import java.util.ArrayList;
import java.util.List;

import com.supermartijn642.tesseract.EnumChannelType;
import com.supermartijn642.tesseract.TesseractBlockEntity;
import com.supermartijn642.tesseract.manager.Channel;
import com.supermartijn642.tesseract.manager.TesseractReference;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

public class CombinedChemicalHandler implements IChemicalHandler {

    private final Channel channel;
    private final TesseractBlockEntity requester;

    public CombinedChemicalHandler(Channel channel, TesseractBlockEntity requester){
        this.channel = channel;
        this.requester = requester;
    }

    @Override
    public int getChemicalTanks(){
        if(this.pushRecurrentCall())
            return 0;

        int tanks = 0;
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities())
                        tanks += Math.max(handler.getChemicalTanks(), 0);
                }
            }
        }

        this.popRecurrentCall();

        return tanks;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank){
        if(this.pushRecurrentCall())
            return ChemicalStack.EMPTY;

        ChemicalStack stack = ChemicalStack.EMPTY;
        int tanks = 0;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            stack = handler.getChemicalInTank(tank - tanks);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();

        return stack;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack){
        if(this.pushRecurrentCall())
            return;

        int tanks = 0;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            handler.setChemicalInTank(tank - tanks, stack);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();
    }

    @Override
    public long getChemicalTankCapacity(int tank){
        if(this.pushRecurrentCall())
            return 0;

        long capacity = 0;
        int tanks = 0;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            capacity = handler.getChemicalTankCapacity(tank - tanks);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();

        return capacity;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack){
        if(this.pushRecurrentCall())
            return false;

        boolean valid = false;
        int tanks = 0;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            valid = handler.isValid(tank - tanks, stack);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();

        return valid;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action){
        if(this.pushRecurrentCall())
            return stack;

        if(!this.requester.canSend(EnumChannelType.CHEMICAL) || stack.isEmpty()){
            this.popRecurrentCall();
            return stack;
        }

        int tanks = 0;
        ChemicalStack ret = stack;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            if(handler.isValid(tank - tanks, stack))
                                ret = handler.insertChemical(tank - tanks, stack, action);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();

        return ret;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack resource, Action action){
        if(this.pushRecurrentCall())
            return resource;

        if(!this.requester.canSend(EnumChannelType.CHEMICAL) || resource.isEmpty()){
            this.popRecurrentCall();
            return resource;
        }

        final List<Distribution> receivers = new ArrayList<Distribution>();
        final long amount = resource.getAmount();
        long inserted = 0;
        long needed;

        // Find all potential receivers and what they need
        for(TesseractReference location : this.channel.receivingTesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if((needed = insertChems(handler, resource, Action.SIMULATE)) > 0)
                            receivers.add(new Distribution(handler, needed));
                    }
                }
            }
        }

        // do the maths
        Distribution.distributeFair(receivers, amount);

        // distribute everyone its fair share
        for(Distribution d : receivers){
            IChemicalHandler handler = (IChemicalHandler) d.handler;
            inserted += insertChems(handler, resource.copyWithAmount(d.given), action);
        }

        this.popRecurrentCall();

        if(inserted >= amount)
            return ChemicalStack.EMPTY;
        return resource.copyWithAmount(resource.getAmount() - inserted);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action){
        if(this.pushRecurrentCall())
            return ChemicalStack.EMPTY;

        if(!this.requester.canReceive(EnumChannelType.CHEMICAL) || amount <= 0){
            this.popRecurrentCall();
            return ChemicalStack.EMPTY;
        }

        int tanks = 0;
        ChemicalStack ret = ChemicalStack.EMPTY;
        loop:
        for(TesseractReference location : this.channel.tesseracts){
            if(location.canBeAccessed()){
                TesseractBlockEntity entity = location.getTesseract();
                if(entity != this.requester){
                    for(IChemicalHandler handler : entity.getSurroundingChemicalCapabilities()){
                        if(tank - tanks < handler.getChemicalTanks()){
                            ret = handler.extractChemical(tank - tanks, amount, action);
                            break loop;
                        }else
                            tanks += Math.max(handler.getChemicalTanks(), 0);
                    }
                }
            }
        }

        this.popRecurrentCall();

        return ret;
    }

    /**
     * Helper method so that insertChemical behaves like FluidHandler and Energy:
     * it returns what was inserted instead of what remains.
     * @param handler The chemical handler to call insertChemical() onto
     * @param stack The chemical stack to insert (not modified)
     * @param action simulate or execute
     * @return The amount that was (or would have been) inserted.
     */
    public static long insertChems(IChemicalHandler handler, ChemicalStack stack, Action action){
        return stack.getAmount() - handler.insertChemical(stack, action).getAmount();
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
