# Flask API untuk prediksi kesehatan mental menggunakan Machine Learning
# Model: Logistic Regression dengan StandardScaler

from flask import Flask, request, jsonify
from flask_cors import CORS
import pickle
import numpy as np
from datetime import datetime
import logging

# Setup logging untuk tracking
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # Izinkan request dari aplikasi Android

# Load model ML dan scaler dari file pickle
try:
    with open('model.pkl', 'rb') as f:
        model = pickle.load(f)
    with open('scaler.pkl', 'rb') as f:
        scaler = pickle.load(f)
    logger.info("Model and scaler loaded successfully")
except Exception as e:
    logger.error(f"Error loading model: {e}")
    model = None
    scaler = None

# Fungsi pemetaan untuk konversi data input
def map_sleep_duration(sleep_str):
    """Map sleep duration string to numeric value"""
    mapping = {
        "Kurang dari 5 jam": 4.0,
        "5-6 jam": 5.5,
        "7-8 jam": 7.5,
        "Lebih dari 8 jam": 9.0
    }
    return mapping.get(sleep_str, 7.5)

def map_dietary_habits(diet_str):
    """Map dietary habits string to numeric value"""
    mapping = {
        "Tidak Sehat": 0,
        "Cukup": 1,
        "Sehat": 2
    }
    return mapping.get(diet_str, 1)

def map_binary(value_str, positive_value):
    """Map binary string to numeric (0 or 1)"""
    return 1 if value_str == positive_value else 0

def generate_advice(prediction, confidence, input_data):
    """Generate personalized advice based on prediction and input data"""

    if prediction == 0:
        # Tidak Ada Depresi - Penguatan positif
        advice = []

        # Saran tidur
        if input_data.get('sleep_duration', 0) >= 7.0:
            advice.append("Pertahankan pola tidur sehat 7-8 jam per malam")
        else:
            advice.append("Cobalah untuk tidur lebih awal dan dapatkan 7-8 jam tidur")

        # Kebiasaan makan
        if input_data.get('dietary_habits', 0) >= 1:
            advice.append("Kebiasaan makan seimbang kamu berkontribusi pada kesejahteraan mental")

        # Kepuasan belajar
        if input_data.get('study_satisfaction', 0) >= 3:
            advice.append("Teruskan tingkat kepuasan studi kamu saat ini")

        # Rekomendasi olahraga
        advice.append("Pertimbangkan olahraga teratur untuk menjaga kesehatan mental")

        # Koneksi sosial
        advice.append("Jaga hubungan sosial dengan teman dan keluarga")

        return advice[:4]  # Return max 4 advice items

    else:
        # Depresi Terdeteksi - Saran yang supportif dan dapat ditindaklanjuti
        advice = []

        # Saran prioritas
        advice.append("🆘 Pertimbangkan berbicara dengan konselor kampus atau profesional kesehatan mental segera")

        # Intervensi tidur
        if input_data.get('sleep_duration', 0) < 7.0:
            advice.append("Prioritaskan tidur 7-8 jam setiap malam - kualitas tidur sangat penting untuk kesehatan mental")

        # Pengecekan pikiran bunuh diri
        if input_data.get('suicidal_thoughts', 0) == 1:
            advice.append("⚠️ Jika kamu memiliki pikiran menyakiti diri, hubungi hotline krisis: 119 ext 8 (24/7)")

        # Tekanan akademik
        if input_data.get('academic_pressure', 0) >= 4:
            advice.append("Bicarakan beban akademik dengan dosen/advisor untuk mencari solusi")

        # Stres finansial
        if input_data.get('financial_stress', 0) >= 4:
            advice.append("Cari tahu bantuan keuangan atau beasiswa yang tersedia di kampus")

        # Dukungan sosial
        advice.append("Hubungi teman, keluarga, atau kelompok dukungan yang dipercaya")

        # Pesan pemberdayaan
        advice.append("💪 Ingat: mencari bantuan adalah tanda kekuatan, bukan kelemahan")

        return advice[:6]  # Return max 6 advice items for high-risk

def determine_risk_level(prediction, confidence):
    """Determine if depression is detected - returns Ya (Yes) or Tidak (No)"""
    if prediction == 0:
        return "Tidak"  # No depression detected
    else:
        return "Ya"  # Depression detected

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "timestamp": datetime.now().isoformat()
    })

@app.route('/predict', methods=['POST'])
def predict():
    """Main prediction endpoint"""

    try:
        # Cek apakah model sudah dimuat
        if model is None or scaler is None:
            return jsonify({
                "success": False,
                "error": "Model not loaded. Please check server configuration."
            }), 500

        # Ambil data JSON
        data = request.get_json()

        if not data:
            return jsonify({
                "success": False,
                "error": "No data provided"
            }), 400

        # Validasi field yang wajib diisi
        required_fields = [
            'gender', 'age', 'academic_pressure', 'study_satisfaction',
            'sleep_duration', 'dietary_habits', 'suicidal_thoughts',
            'study_hours', 'financial_stress', 'family_history'
        ]

        missing_fields = [field for field in required_fields if field not in data]
        if missing_fields:
            return jsonify({
                "success": False,
                "error": f"Missing required fields: {', '.join(missing_fields)}"
            }), 400

        # Proses data input
        # Pemetaan nilai string ke numerik
        sleep_numeric = map_sleep_duration(data['sleep_duration']) if isinstance(data['sleep_duration'], str) else data['sleep_duration']
        diet_numeric = map_dietary_habits(data['dietary_habits']) if isinstance(data['dietary_habits'], str) else data['dietary_habits']

        # Pemetaan nilai biner
        gender_numeric = map_binary(data['gender'], "Laki-laki") if isinstance(data['gender'], str) else data['gender']
        suicidal_numeric = map_binary(data['suicidal_thoughts'], "Pernah") if isinstance(data['suicidal_thoughts'], str) else data['suicidal_thoughts']
        family_history_numeric = map_binary(data['family_history'], "Ya") if isinstance(data['family_history'], str) else data['family_history']

        # Buat array fitur dalam urutan yang benar
        # [gender, age, academic_pressure, study_satisfaction, sleep_duration,
        #  dietary_habits, suicidal_thoughts, study_hours, financial_stress, family_history]
        features = np.array([[
            gender_numeric,
            float(data['age']),
            float(data['academic_pressure']),
            float(data['study_satisfaction']),
            sleep_numeric,
            diet_numeric,
            suicidal_numeric,
            float(data['study_hours']),
            float(data['financial_stress']),
            family_history_numeric
        ]])

        logger.info(f"Raw features: {features}")

        # Skalakan fitur
        features_scaled = scaler.transform(features)
        logger.info(f"Scaled features: {features_scaled}")

        # Buat prediksi
        prediction = int(model.predict(features_scaled)[0])

        # Ambil probabilitas prediksi
        try:
            probabilities = model.predict_proba(features_scaled)[0]
            confidence = float(probabilities[prediction])
        except:
            confidence = 0.75  # Default confidence if predict_proba not available

        logger.info(f"Prediction: {prediction}, Confidence: {confidence}")

        # Generate label prediksi
        prediction_label = "Tidak Ada Depresi" if prediction == 0 else "Risiko Depresi Terdeteksi"

        # Tentukan tingkat risiko
        risk_level = determine_risk_level(prediction, confidence)

        # Siapkan dict data input untuk generate saran
        input_data_dict = {
            'sleep_duration': sleep_numeric,
            'dietary_habits': diet_numeric,
            'study_satisfaction': float(data['study_satisfaction']),
            'suicidal_thoughts': suicidal_numeric,
            'academic_pressure': float(data['academic_pressure']),
            'financial_stress': float(data['financial_stress'])
        }

        # Generate saran yang dipersonalisasi
        advice = generate_advice(prediction, confidence, input_data_dict)

        # Siapkan response
        response = {
            "success": True,
            "prediction": prediction,
            "prediction_label": prediction_label,
            "confidence": round(confidence, 2),
            "risk_level": risk_level,
            "advice": advice,
            "timestamp": datetime.now().isoformat()
        }

        logger.info(f"Response: {response}")

        return jsonify(response), 200

    except Exception as e:
        logger.error(f"Error during prediction: {str(e)}", exc_info=True)
        return jsonify({
            "success": False,
            "error": f"Internal server error: {str(e)}"
        }), 500

@app.route('/test', methods=['GET'])
def test():
    """Test endpoint with sample data"""
    sample_data = {
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
    }

    # Buat prediksi test
    with app.test_client() as client:
        response = client.post('/predict',
                              json=sample_data,
                              content_type='application/json')
        return response.get_json()

if __name__ == '__main__':
    # Diubah ke port 5001 karena macOS AirPlay Receiver menggunakan port 5000
    app.run(host='0.0.0.0', port=5001, debug=True)
