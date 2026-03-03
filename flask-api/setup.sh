#!/bin/bash

echo "🚀 Setting up Flask API for MindCheck..."
echo ""

# Rename model files if needed
if [ -f "logistic_regression_model.pkl" ] && [ ! -f "model.pkl" ]; then
    echo "📦 Renaming logistic_regression_model.pkl → model.pkl"
    mv logistic_regression_model.pkl model.pkl
fi

if [ -f "standard_scaler.pkl" ] && [ ! -f "scaler.pkl" ]; then
    echo "📦 Renaming standard_scaler.pkl → scaler.pkl"
    mv standard_scaler.pkl scaler.pkl
fi

# Check if model files exist
if [ ! -f "model.pkl" ]; then
    echo "❌ Error: model.pkl not found!"
    echo "   Please copy your trained model to flask-api/model.pkl"
    exit 1
fi

if [ ! -f "scaler.pkl" ]; then
    echo "❌ Error: scaler.pkl not found!"
    echo "   Please copy your scaler to flask-api/scaler.pkl"
    exit 1
fi

echo "✅ Model files found"
echo ""

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    echo "✅ Virtual environment created"
else
    echo "✅ Virtual environment already exists"
fi

echo ""
echo "🔧 Activating virtual environment..."
source venv/bin/activate

echo "📥 Installing dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

echo ""
echo "✅ Setup complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Activate venv:  source venv/bin/activate"
echo "   2. Start server:   python app.py"
echo "   3. Test API:       curl http://localhost:5000/health"
echo ""
