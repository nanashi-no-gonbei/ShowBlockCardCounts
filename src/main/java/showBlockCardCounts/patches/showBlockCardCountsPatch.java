package showBlockCardCounts.patches;

//import ThePokerPlayer.patches.CardTypeEnum;
import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.KeywordStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import javassist.CtBehavior;
import showBlockCardCounts.ShowBlockCardCounts;

import java.util.Arrays;
import java.util.Locale;


@SpirePatch2(
        clz = CardRewardScreen.class,
        method = "render"
)
public class showBlockCardCountsPatch {

    private static final KeywordStrings keyWordsStrings = CardCrawlGame.languagePack.getKeywordString("Game Dictionary");

    public static void countCardTypes(SpriteBatch sb) {
        int blockCardCounts = 0;
        String outString = "";
        int deckSize = 0;

        boolean countCurses = ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_CURSES_SETTING);
        boolean countAscendersBane = ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_ASCENDERS_BANE_SETTING);
        boolean showPercentages = ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_PERCENTAGES_SETTING);
        boolean showOnRight = ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_SHOW_ON_RIGHT_SETTING);

        for (AbstractCard c: AbstractDungeon.player.masterDeck.group) {

            if(isBlockCard(c)){
                blockCardCounts++;
            }
            switch (c.type) {
                case ATTACK:
                case SKILL:
                case POWER:
                    deckSize++;
                    break;
                case CURSE:
                    if (countCurses && !(c.cardID.equals("AscendersBane") && !countAscendersBane)) {
                        deckSize++;
                    }
                    break;
                case STATUS:
                    if (countCurses) {
                        deckSize++;
                    }
                    break;
                default:
                    break;
            }
        }

        try{
            int blockPercentageNum;
            if(deckSize != 0){
                blockPercentageNum = Math.round( (float) blockCardCounts * 100 / deckSize);
            }else{
                blockPercentageNum = 0;
            }
            String blockPercentage = showPercentages ? String.format(" (%d%%)", blockPercentageNum) : "";
            outString = outString.concat(String.format("%1$s: %2$d%3$s\n", keyWordsStrings.BLOCK.NAMES[0], blockCardCounts, blockPercentage).toLowerCase(Locale.ROOT));
        }catch(Exception e){
            e.printStackTrace();
        }

        // render output, on correct side of screen
        if (showOnRight) {
            FontHelper.renderFontRightAligned(sb, FontHelper.panelNameFont, outString, Settings.WIDTH - 16f * Settings.scale, Settings.HEIGHT * 3.0F / 4.0F, Color.WHITE.cpy());
        } else {
            FontHelper.renderFontLeft(sb, FontHelper.panelNameFont, outString, 16f * Settings.scale, Settings.HEIGHT * 3.0F / 4.0F, Color.WHITE.cpy());
        }
    }

    @SpirePatch2(
            clz = CardRewardScreen.class,
            method = "render"
    )
    public static class cardRewardScreenPatch {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = {"sb"}
        )
        public static void Insert(SpriteBatch sb) {
            if (ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_ON_CARD_REWARDS_SETTING)) {
                countCardTypes(sb);
            }
        }

        private static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(CardRewardScreen.class,"renderTwitchVotes");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch2(
            clz = MasterDeckViewScreen.class,
            method = "render"
    )
    public static class deckViewScreenPatch {
        @SpirePostfixPatch
        public static void Postfix(SpriteBatch sb) {
            if (ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_ON_VIEW_DECK_SETTING)) {
                countCardTypes(sb);
            }
        }
    }

    @SpirePatch2(
            clz = ShopScreen.class,
            method = "render"
    )
    public static class shopScreenPatch {
        @SpirePostfixPatch
        public static void Postfix(SpriteBatch sb) {
            if (ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_ON_SHOP_SETTING)) {
                countCardTypes(sb);
            }
        }
    }

    @SpirePatch2(
            clz = GridCardSelectScreen.class,
            method = "render"
    )
    public static class GridCardSelectScreenPatch {
        @SpirePostfixPatch
        public static void Postfix(GridCardSelectScreen __instance, SpriteBatch sb) {
            // this is where I would check __instance.forTransform if it was actually used... (along with forUpgrade and forPurge)
            // this displays if the peek button is hidden, which effectively means out of combat, so catches everything with like Cursed Bell as the only false positive
            if (ShowBlockCardCounts.showBlockCardCountsConfig.getBool(ShowBlockCardCounts.ENABLE_ON_REMOVE_SETTING) && (boolean)ReflectionHacks.getPrivate(__instance.peekButton, PeekButton.class, "isHidden")) {
                countCardTypes(sb);
            }
        }
    }
    public static boolean isBlockCard(AbstractCard c){

        if(c == null) return false;
        // note: consider frost orb cards as block cards except for 'Chaos'.
        // note: not consider buffs/debuffs cards as block cards such as 'Rage','TalkToTheHand', and so on.
        String[] blockCardIDsWithoutPositiveBaseBlock = {"Entrench","Stack",
                                                        "Chill","Cold Snap","Coolheaded","Glacier","Rainbow",
                                                        "SpiritShield","Wallop"
                                                        };
        // Exclude buff card with positive baseBlock.
        return (c.baseBlock > 0 && !(c.cardID.equals("Wish"))) ||
            (Arrays.asList(blockCardIDsWithoutPositiveBaseBlock).contains(c.cardID));
    }


}
