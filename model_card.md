# Model Card — CartPlan Product Recommendation Model

**Project:** CartPlan  
**Model status:** Proposed design / prototype stage  
**Model type:** Hybrid recommender system (Content-Based Filtering + Collaborative Filtering)  
**Document purpose:** Describe the intended model and its role in CartPlan. This document does not claim that the model has already been trained or deployed.

## 1. Model overview

The CartPlan recommendation model is intended to suggest products that may be useful to a user when they create or update a shopping list. It uses two complementary signals:

1. **Content-based component:** estimates similarity between products using available product attributes and taxonomy (for example, category, aisle, department, and other attributes available in the data).
2. **Collaborative component:** identifies products that are often purchased together or by users with similar purchase patterns, using aggregated order histories.

The components are combined into a ranked list. The exact weighting and algorithm configuration will be selected during offline experiments and recorded with each model version.

## 2. Intended use

### Primary use
- Recommend relevant products to a CartPlan user while preparing a shopping list.
- Help users discover products they may have forgotten or may commonly purchase together with items already in the list.
- Provide a ranked set of suggestions that the user can accept or ignore.

### Out of scope
- Automatically adding products to a shopping list without user confirmation.
- Predicting medical, dietary, financial, or other sensitive characteristics.
- Guaranteeing product availability, current prices, or suitability for a particular person.
- Replacing the user's decision or acting as a nutrition or health adviser.

## 3. Problem statement

A user may not remember every product needed for a planned shopping trip. CartPlan can use patterns in previous baskets and relationships between products to generate contextually relevant suggestions.

This is a ranking/recommendation task: given a user's recent purchase context and/or the current list, rank candidate products by estimated relevance.

## 4. Users and stakeholders

- **End user:** a CartPlan user who creates and manages shopping lists in the Android or web application.
- **Application:** CartPlan backend, which receives recommendation requests and returns ranked suggestions through an API.
- **Project team:** responsible for data preparation, model evaluation, deployment, monitoring, and version management.

## 5. Input data

The planned model may use the following data, subject to availability and data-quality checks:

| Input | Description | Example |
|---|---|---|
| Current list | Products already added to the active list | milk, bread |
| Purchase history | Previous orders associated with the user | product IDs and order sequence |
| Product identifier | Stable product ID | product_id |
| Product taxonomy | Category, aisle, department or equivalent attributes | dairy, bakery |
| Interaction signals | Purchase/reorder events and their frequency | product was reordered |
| Context | Optional non-sensitive context, such as list contents | current basket |

For the prototype, the planned public dataset is **Instacart Market Basket Analysis**. It contains anonymized order-level purchase records and product/category information. It is an experimental source, not CartPlan user data.

For production use, the model must be trained and evaluated using appropriately collected CartPlan interaction and purchase history, subject to consent, privacy, retention, and access rules.

## 6. Output

The model returns a ranked list of candidate products, for example:

- product ID;
- relevance score or rank;
- optional reason/category label, if supported by the implementation.

The backend maps product IDs to CartPlan catalogue records and returns display-ready data to the client. The Android or web application displays suggestions; the user decides whether to add an item.

The model does not create or modify a shopping list by itself.

## 7. Model architecture and inference flow

1. The client sends the current list context and an authorized user/context identifier to the CartPlan backend.
2. The backend validates the request and obtains the required product/history features.
3. The content-based component scores candidate products by similarity to the current list and relevant product attributes.
4. The collaborative component scores candidates using co-occurrence and purchase-pattern signals.
5. The hybrid ranking combines the component scores, applies exclusions (for example, products already on the list), and selects the top-N candidates.
6. The backend returns ranked product suggestions through the recommendation API.
7. The client displays the suggestions and records user interactions where appropriate.

The model is intended to run server-side so Android and web clients use the same model version and recommendation logic.

## 8. Training data and preparation

### Prototype
The proposed prototype uses Instacart Market Basket Analysis. Preparation steps:
- inspect schema, data volume, missing values, and duplicate records;
- map order records to product identifiers;
- build product co-occurrence and user/order interaction features;
- use a time-aware or order-based train/validation/test split to reduce information leakage;
- ensure that evaluation uses only information available before the target order.

### Production
The production dataset is expected to consist of CartPlan purchase/list interaction events, after the application has accumulated sufficient reliable history. User-level data must be access-controlled and processed in accordance with applicable privacy requirements.

## 9. Evaluation

No measured quality results are claimed at this stage. The following metrics are proposed for offline evaluation:

| Metric | What it measures | Why it is useful |
|---|---|---|
| Precision@K | Share of top-K recommendations that are relevant | Controls irrelevant suggestions |
| Recall@K | Share of relevant items recovered in top K | Measures how many useful items are found |
| MAP@K | Ranking quality across relevant items | Rewards placing relevant items higher |
| NDCG@K | Position-sensitive ranking quality | Gives more credit to relevant items near the top |
| Catalog coverage | Share of catalogue items that can be recommended | Detects overly narrow recommendations |

The model should be compared with a simple popularity/frequency baseline. Metrics, K values, split strategy, and acceptance thresholds must be fixed before model comparison. Final thresholds will be set after establishing a baseline; they are not invented in this card.

Online monitoring, when sufficient traffic exists, may additionally track recommendation impressions, click/add-to-list rate, dismissal rate, and latency. These product metrics complement offline ranking metrics and do not replace them.

## 10. Limitations and risks

- **Cold start:** new users and products have little or no interaction history. The content-based component and popularity fallback can help, but may not fully solve the problem.
- **Sparse data:** many users may have short histories, limiting collaborative signals.
- **Dataset mismatch:** Instacart data reflects another service and user population. Prototype results may not transfer to CartPlan.
- **Catalogue mismatch:** public dataset taxonomy and products may differ from the CartPlan catalogue.
- **Popularity bias:** frequent products may dominate recommendations, reducing diversity.
- **Stale data:** purchase preferences and catalogue contents can change.
- **Privacy:** production training must use only permitted data, with appropriate access controls and minimization.
- **No guarantee of correctness:** recommendations are suggestions, not proof that a user needs or wants an item.

## 11. Fairness, privacy, and responsible use

The model is not intended to infer protected or sensitive characteristics. It should use only data necessary for product recommendation. User histories must not be exposed to other users. In production, the backend must enforce authentication and authorization before accessing personal histories; this is a production requirement and is not claimed as an already implemented MVP capability.

## 12. Human oversight

The user remains in control. Recommendations are displayed as optional suggestions and are added to a list only after an explicit user action.

## 13. Deployment and monitoring

The target architecture places the model and inference API on the CartPlan server. Android and web clients call the same API. The model artifact must be versioned. Deployment should follow validation in a test environment, a controlled rollout, monitoring, and rollback capability.

The prototype may be developed and evaluated locally before server integration. The presence of this design document does not mean that model training, API integration, or production deployment has already taken place.

## 14. Version history

| Version | Status | Description |
|---|---|---|
| 0.1 | Proposed | Initial design of a hybrid product recommendation model for CartPlan |
