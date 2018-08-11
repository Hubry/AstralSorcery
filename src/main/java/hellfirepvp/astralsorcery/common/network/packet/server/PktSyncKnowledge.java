/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2018
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.network.packet.server;

import com.google.common.collect.Lists;
import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.common.constellation.ConstellationRegistry;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.IMajorConstellation;
import hellfirepvp.astralsorcery.common.constellation.perk.AbstractPerk;
import hellfirepvp.astralsorcery.common.constellation.perk.tree.PerkTree;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.data.research.ResearchProgression;
import hellfirepvp.astralsorcery.common.item.tool.sextant.SextantFinder;
import hellfirepvp.astralsorcery.common.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: PktSyncKnowledge
 * Created by HellFirePvP
 * Date: 07.05.2016 / 13:34
 */
public class PktSyncKnowledge implements IMessage, IMessageHandler<PktSyncKnowledge, IMessage> {

    public static final byte STATE_ADD = 0;
    public static final byte STATE_WIPE = 1;

    private byte state;
    public List<String> knownConstellations = new ArrayList<>();
    public List<String> seenConstellations = new ArrayList<>();
    public List<ResearchProgression> researchProgression = new ArrayList<>();
    public List<SextantFinder.TargetObject> usedTargets = new ArrayList<>();
    public IMajorConstellation attunedConstellation = null;
    public int progressTier = 0;
    public boolean wasOnceAttuned = false;
    public Map<AbstractPerk, NBTTagCompound> usedPerks = new HashMap<>();
    public List<String> freePointTokens = Lists.newArrayList();
    public double perkExp = 0;

    public PktSyncKnowledge() {}

    public PktSyncKnowledge(byte state) {
        this.state = state;
    }

    public void load(PlayerProgress progress) {
        this.knownConstellations = progress.getKnownConstellations();
        this.seenConstellations = progress.getSeenConstellations();
        this.researchProgression = progress.getResearchProgression();
        this.progressTier = progress.getTierReached().ordinal();
        this.attunedConstellation = progress.getAttunedConstellation();
        this.freePointTokens = progress.getFreePointTokens();
        this.usedPerks = progress.getUnlockedPerkData();
        this.perkExp = progress.getPerkExp();
        this.wasOnceAttuned = progress.wasOnceAttuned();
        this.usedTargets = progress.getUsedTargets();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.state = buf.readByte();

        int cLength = buf.readInt();
        if (cLength != -1) {
            knownConstellations = new ArrayList<>(cLength);
            for (int i = 0; i < cLength; i++) {
                String val = ByteBufUtils.readString(buf);
                knownConstellations.add(val);
            }
        } else {
            knownConstellations = new ArrayList<>();
        }

        cLength = buf.readInt();
        if (cLength != -1) {
            seenConstellations = new ArrayList<>(cLength);
            for (int i = 0; i < cLength; i++) {
                String val = ByteBufUtils.readString(buf);
                seenConstellations.add(val);
            }
        } else {
            seenConstellations = new ArrayList<>();
        }

        int rLength = buf.readInt();
        if (rLength != -1) {
            researchProgression = new ArrayList<>(rLength);
            for (int i = 0; i < rLength; i++) {
                researchProgression.add(ResearchProgression.getById(buf.readInt()));
            }
        } else {
            researchProgression = new ArrayList<>();
        }

        int attunementPresent = buf.readByte();
        if(attunementPresent != -1) {
            String attunement = ByteBufUtils.readString(buf);
            IConstellation c = ConstellationRegistry.getConstellationByName(attunement);
            if(c == null || !(c instanceof IMajorConstellation)) {
                AstralSorcery.log.warn("[AstralSorcery] received constellation-attunement progress-packet with unknown constellation: " + attunement);
            } else {
                this.attunedConstellation = (IMajorConstellation) c;
            }
        }

        int perkLength = buf.readInt();
        if(perkLength != -1) {
            this.usedPerks = new HashMap<>();
            for (int i = 0; i < perkLength; i++) {
                String key = ByteBufUtils.readString(buf);
                NBTTagCompound tag = ByteBufUtils.readNBTTag(buf);
                AbstractPerk perk = PerkTree.PERK_TREE.getPerk(new ResourceLocation(key));
                if (perk != null) {
                    this.usedPerks.put(perk, tag);
                }
            }
        } else {
            this.usedPerks = new HashMap<>();
        }

        int targetLength = buf.readInt();
        if (targetLength != -1) {
            this.usedTargets = new ArrayList<>(targetLength);
            for (int i = 0; i < targetLength; i++) {
                String str = ByteBufUtils.readString(buf);
                SextantFinder.TargetObject to = SextantFinder.getByName(str);
                if (to != null) {
                    this.usedTargets.add(to);
                }
            }
        } else {
            this.usedTargets = Lists.newArrayList();
        }

        int tokenLength = buf.readInt();
        if (tokenLength != -1) {
            this.freePointTokens = new ArrayList<>(tokenLength);
            for (int i = 0; i < tokenLength; i++) {
                this.freePointTokens.add(ByteBufUtils.readString(buf));
            }
        } else {
            this.freePointTokens = Lists.newArrayList();
        }

        this.wasOnceAttuned = buf.readBoolean();
        this.progressTier = buf.readInt();
        this.perkExp = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(state);

        if (knownConstellations != null) {
            buf.writeInt(knownConstellations.size());
            for (String dat : knownConstellations) {
                ByteBufUtils.writeString(buf, dat);
            }
        } else {
            buf.writeInt(-1);
        }

        if (seenConstellations != null) {
            buf.writeInt(seenConstellations.size());
            for (String dat : seenConstellations) {
                ByteBufUtils.writeString(buf, dat);
            }
        } else {
            buf.writeInt(-1);
        }

        if (researchProgression != null) {
            buf.writeInt(researchProgression.size());
            for (ResearchProgression progression : researchProgression) {
                buf.writeInt(progression.getProgressId());
            }
        } else {
            buf.writeInt(-1);
        }

        if(attunedConstellation != null) {
            buf.writeByte(1);
            ByteBufUtils.writeString(buf, attunedConstellation.getUnlocalizedName());
        } else {
            buf.writeByte(-1);
        }

        if(usedPerks != null) {
            buf.writeInt(usedPerks.size());
            for (Map.Entry<AbstractPerk, NBTTagCompound> perkEntry : usedPerks.entrySet()) {
                ByteBufUtils.writeString(buf, perkEntry.getKey().getRegistryName().toString());
                ByteBufUtils.writeNBTTag(buf, perkEntry.getValue());
            }
        } else {
            buf.writeInt(-1);
        }

        if(usedTargets != null) {
            buf.writeInt(usedTargets.size());
            for (SextantFinder.TargetObject to : usedTargets) {
                ByteBufUtils.writeString(buf, to.getRegistryName());
            }
        } else {
            buf.writeInt(-1);
        }

        if (freePointTokens != null) {
            buf.writeInt(freePointTokens.size());
            for (String token : freePointTokens) {
                ByteBufUtils.writeString(buf, token);
            }
        } else {
            buf.writeInt(-1);
        }

        buf.writeBoolean(this.wasOnceAttuned);
        buf.writeInt(this.progressTier);
        buf.writeDouble(this.perkExp);
    }

    @Override
    public PktSyncKnowledge onMessage(PktSyncKnowledge message, MessageContext ctx) {
        receiveMessageClient(message, ctx);
        return null;
    }

    @SideOnly(Side.CLIENT)
    private void receiveMessageClient(PktSyncKnowledge message, MessageContext ctx) {
        AstralSorcery.proxy.scheduleClientside(() -> {
            switch (message.state) {
                case STATE_ADD:
                    ResearchManager.recieveProgressFromServer(message);
                    break;
                case STATE_WIPE:
                    ResearchManager.clientProgress = new PlayerProgress();
                    break;
            }
        });
    }
}
