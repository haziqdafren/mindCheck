"""
Script to create dummy model and scaler for testing
Replace model.pkl and scaler.pkl with your actual trained model files
"""

import pickle
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler

# Create dummy data for training
# 10 features: gender, age, academic_pressure, study_satisfaction, sleep_duration,
#              dietary_habits, suicidal_thoughts, study_hours, financial_stress, family_history
np.random.seed(42)

# Generate 100 dummy samples
n_samples = 100
X = np.random.randn(n_samples, 10)

# Adjust feature ranges to be realistic
X[:, 0] = np.random.randint(0, 2, n_samples)  # gender (0 or 1)
X[:, 1] = np.random.randint(18, 31, n_samples)  # age (18-30)
X[:, 2] = np.random.uniform(1, 5, n_samples)  # academic_pressure (1-5)
X[:, 3] = np.random.uniform(1, 5, n_samples)  # study_satisfaction (1-5)
X[:, 4] = np.random.choice([4.0, 5.5, 7.5, 9.0], n_samples)  # sleep_duration
X[:, 5] = np.random.randint(0, 3, n_samples)  # dietary_habits (0-2)
X[:, 6] = np.random.randint(0, 2, n_samples)  # suicidal_thoughts (0 or 1)
X[:, 7] = np.random.randint(0, 13, n_samples)  # study_hours (0-12)
X[:, 8] = np.random.uniform(1, 5, n_samples)  # financial_stress (1-5)
X[:, 9] = np.random.randint(0, 2, n_samples)  # family_history (0 or 1)

# Generate target labels (0 = No Depression, 1 = Depression)
# Simple rule: if academic_pressure > 3.5 AND sleep_duration < 6 AND suicidal_thoughts == 1, likely depression
y = ((X[:, 2] > 3.5) & (X[:, 4] < 6) & (X[:, 6] == 1)).astype(int)

# Add some randomness
y = np.where(np.random.rand(n_samples) < 0.1, 1 - y, y)

print(f"Training samples: {n_samples}")
print(f"Depression cases: {y.sum()}")
print(f"No depression cases: {(1-y).sum()}")

# Create and train scaler
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# Create and train logistic regression model
model = LogisticRegression(random_state=42, max_iter=1000)
model.fit(X_scaled, y)

# Calculate accuracy
accuracy = model.score(X_scaled, y)
print(f"Model training accuracy: {accuracy:.2f}")

# Save model and scaler
with open('model.pkl', 'wb') as f:
    pickle.dump(model, f)
print("✓ Saved model.pkl")

with open('scaler.pkl', 'wb') as f:
    pickle.dump(scaler, f)
print("✓ Saved scaler.pkl")

# Test prediction
test_sample = np.array([[
    1,      # gender (Laki-laki)
    21,     # age
    4.0,    # academic_pressure
    3.0,    # study_satisfaction
    5.5,    # sleep_duration (5-6 jam)
    1,      # dietary_habits (Cukup)
    0,      # suicidal_thoughts (Tidak Pernah)
    8,      # study_hours
    3.0,    # financial_stress
    0       # family_history (Tidak)
]])

test_scaled = scaler.transform(test_sample)
prediction = model.predict(test_scaled)[0]
probability = model.predict_proba(test_scaled)[0]

print(f"\nTest prediction:")
print(f"  Input: {test_sample}")
print(f"  Prediction: {prediction} ({'Tidak Ada Depresi' if prediction == 0 else 'Depresi Terdeteksi'})")
print(f"  Confidence: {probability[prediction]:.2f}")
print(f"  Probabilities: {probability}")

print("\n✅ Dummy model created successfully!")
print("⚠️  Remember to replace model.pkl and scaler.pkl with your actual trained model files")
