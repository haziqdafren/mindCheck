# Flask API Setup Instructions

## Step 1: Install Dependencies

```bash
cd flask-api
pip3 install -r requirements.txt
```

## Step 2: Copy Your Model Files

**IMPORTANT**: Copy your trained model files to this directory:

```bash
# Copy your files here:
# flask-api/model.pkl    <- Your trained Logistic Regression model
# flask-api/scaler.pkl   <- Your StandardScaler
```

Expected files:
- `model.pkl` - Trained Logistic Regression model
- `scaler.pkl` - StandardScaler for feature normalization

## Step 3: Test the API

### Start the server:
```bash
python3 app.py
```

You should see:
```
 * Running on http://0.0.0.0:5000
```

### Test health check:
```bash
curl http://localhost:5000/health
```

Expected response:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "timestamp": "2025-11-26T..."
}
```

### Test prediction:
```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "gender": "Laki-laki",
    "age": 21,
    "academic_pressure": 4,
    "study_satisfaction": 3,
    "sleep_duration": "5-6 jam",
    "dietary_habits": "Cukup",
    "suicidal_thoughts": "Tidak Pernah",
    "study_hours": 8,
    "financial_stress": 3,
    "family_history": "Tidak"
  }'
```

### Quick test (uses built-in test data):
```bash
curl http://localhost:5000/test
```

## Troubleshooting

### Error: "Model not loaded"
- Make sure `model.pkl` and `scaler.pkl` are in the `flask-api/` directory
- Check file names are exactly: `model.pkl` and `scaler.pkl` (lowercase)
- Verify files are valid pickle files

### Import errors:
```bash
# Reinstall dependencies
pip3 install --upgrade -r requirements.txt
```

### Port already in use:
```bash
# Change port in app.py, line: app.run(host='0.0.0.0', port=5000, debug=True)
# Change 5000 to another port like 5001
```

## Next Steps

Once API is running successfully:
1. Keep the terminal open (API server running)
2. Note the URL: `http://localhost:5000`
3. We'll configure Android app to use this URL
