import pandas as pd
import pytest

from cartplan_ml.models.popularity import PopularityRecommender


@pytest.fixture
def interactions() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "user_id": [1, 1, 2, 2, 3, 3, 3],
            "item_id": [10, 20, 10, 30, 10, 20, 40],
        }
    )


def test_recommendations_exclude_purchased_items(interactions):
    model = PopularityRecommender().fit(interactions)

    recommendations = model.recommend(user_id=1)

    recommended_ids = [item["item_id"] for item in recommendations]

    assert 10 not in recommended_ids
    assert 20 not in recommended_ids


def test_recommendations_sorted_by_popularity(interactions):
    model = PopularityRecommender().fit(interactions)

    recommendations = model.recommend(user_id=999)

    assert recommendations[0]["item_id"] == 10
    assert recommendations[0]["purchase_count"] == 3


def test_recommendations_respect_limit(interactions):
    model = PopularityRecommender().fit(interactions)

    recommendations = model.recommend(user_id=999, n=2)

    assert len(recommendations) == 2


def test_recommendation_requires_fitted_model():
    model = PopularityRecommender()

    with pytest.raises(RuntimeError):
        model.recommend(user_id=1)


def test_fit_rejects_missing_columns():
    model = PopularityRecommender()
    invalid_data = pd.DataFrame({"user_id": [1]})

    with pytest.raises(ValueError):
        model.fit(invalid_data)


def test_fit_rejects_empty_dataset():
    model = PopularityRecommender()
    empty_data = pd.DataFrame(columns=["user_id", "item_id"])

    with pytest.raises(ValueError):
        model.fit(empty_data)
