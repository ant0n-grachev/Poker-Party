package players;

import game.*;

public class Anton extends Player {

    public Anton(String name) {
        super(name);
    }

    @Override
    protected void takePlayerTurn() {
        if (evaluateHandStrength() >= 0.8) allIn();
        else if (evaluateHandStrength() >= 0.6) raise(getGameState().getTableMinBet() * 2);
        else if (evaluateHandStrength() >= 0.4) raise(getGameState().getTableMinBet());
        else if (getGameState().isActiveBet() && evaluateHandStrength() >= 0.2) call();
        else if (!getGameState().isActiveBet()) check();
        else fold();
    }

    @Override
    protected boolean shouldFold() {
        return false;
    }

    @Override
    protected boolean shouldCheck() {
        return false;
    }

    @Override
    protected boolean shouldCall() {
        return false;
    }

    @Override
    protected boolean shouldRaise() {
        return false;
    }

    @Override
    protected boolean shouldAllIn() {
        return false;
    }


    private double evaluateHandStrength() {
        Evaluator evaluator = new Evaluator();
        HandRanks handRank = evaluator.evaluatePlayerHand(getHandCards(), getGameState().getTableCards());

        // Assign a strength value to each hand rank
        double[] handStrengthValues = {
                0.0, // HIGH_CARD
                0.2, // PAIR
                0.4, // TWO_PAIR
                0.6, // THREE_OF_A_KIND
                0.7, // STRAIGHT
                0.8, // FLUSH
                0.9, // FULL_HOUSE
                1.0, // FOUR_OF_A_KIND
                1.1, // STRAIGHT_FLUSH
                1.2  // ROYAL_FLUSH
        };

        return handStrengthValues[handRank.getValue() - 1];
    }
}
