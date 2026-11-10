from __future__ import annotations

import pandas as pd


class PopularityRecommender:
    """Recommend popular products while excluding items already purchased."""

    REQUIRED_COLUMNS = {"user_id", "item_id"}

    def __init__(self) -> None:
        self._interactions: pd.DataFrame | None = None
        self._popularity: pd.DataFrame | None = None

    def fit(self, interactions: pd.DataFrame) -> PopularityRecommender:
        """Calculate product popularity from historical interactions."""
        missing = self.REQUIRED_COLUMNS - set(interactions.columns)
        if missing:
            raise ValueError(f"Missing required columns: {sorted(missing)}")

        data = interactions[["user_id", "item_id"]].dropna().copy()
        if data.empty:
            raise ValueError("Interactions dataset is empty.")

        self._interactions = data
        self._popularity = (
            data.groupby("item_id", sort=False)
            .size()
            .reset_index(name="purchase_count")
            .sort_values("purchase_count", ascending=False, kind="stable")
            .reset_index(drop=True)
        )
        return self

    def recommend(self, user_id: object, n: int = 10) -> list[dict[str, object]]:
        """Return the top-N popular products not yet purchased by a user."""
        if self._interactions is None or self._popularity is None:
            raise RuntimeError("The model must be fitted before making recommendations.")

        if n < 1:
            raise ValueError("n must be greater than zero.")

        user_items = self._interactions.loc[self._interactions["user_id"] == user_id, "item_id"]
        seen_items = set(user_items)

        recommendations = self._popularity[~self._popularity["item_id"].isin(seen_items)].head(n)

        return recommendations.to_dict(orient="records")
