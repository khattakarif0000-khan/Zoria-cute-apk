const http = require('http');

const port = process.env.PORT || process.env.DEFAULT_APP_PORT || 3000;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`<!DOCTYPE html>
<html lang="ur" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ZORIA - ذہین آواز معاون</title>
  <style>
    body {
      margin: 0;
      padding: 24px;
      background: #070A14;
      color: #F8FAFC;
      font-family: system-ui, -apple-system, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
      box-sizing: border-box;
    }
    .card {
      background: #0F172A;
      border: 1px solid #1E293B;
      border-radius: 20px;
      padding: 32px;
      max-width: 640px;
      width: 100%;
      box-shadow: 0 12px 36px rgba(0,0,0,0.5);
      text-align: center;
    }
    .orb {
      width: 110px;
      height: 110px;
      border-radius: 50%;
      background: radial-gradient(circle, #00E5FF 0%, #7C4DFF 70%, #FF4081 100%);
      margin: 0 auto 20px;
      box-shadow: 0 0 35px rgba(124, 77, 255, 0.6);
      animation: pulse 3s infinite ease-in-out;
    }
    @keyframes pulse {
      0%, 100% { transform: scale(0.96); opacity: 0.9; }
      50% { transform: scale(1.06); opacity: 1; }
    }
    h1 { margin: 0 0 8px; font-size: 32px; color: #FFFFFF; letter-spacing: 1px; }
    .tagline { color: #94A3B8; font-size: 16px; margin-bottom: 24px; }
    .greeting {
      background: #1E293B;
      border-right: 4px solid #7C4DFF;
      padding: 16px;
      border-radius: 12px;
      margin: 20px 0;
      font-size: 18px;
      color: #F1F5F9;
      line-height: 1.6;
    }
    .badge-container { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
    .badge {
      background: rgba(0, 229, 255, 0.12);
      border: 1px solid rgba(0, 229, 255, 0.35);
      color: #00E5FF;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
    }
    .badge.voice {
      background: rgba(255, 64, 129, 0.12);
      border-color: rgba(255, 64, 129, 0.35);
      color: #FF4081;
    }
    .info {
      font-size: 14px;
      color: #94A3B8;
      line-height: 1.6;
      border-top: 1px solid #1E293B;
      padding-top: 20px;
      text-align: right;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="orb"></div>
    <h1>ZORIA</h1>
    <div class="tagline">ذہین آواز معاون اور ہم سفر (AI Voice Assistant)</div>
    
    <div class="badge-container">
      <span class="badge">بنیادی زبان: اردو (Urdu-First)</span>
      <span class="badge voice">آواز: Kore & Aoede</span>
      <span class="badge">Gemini AI Brain</span>
      <span class="badge">4 پرسنالٹی موڈز</span>
      <span class="badge">فون و واٹس ایپ کنٹرول</span>
    </div>

    <div class="greeting">
      «السلام علیکم Boss، میں ZORIA ہوں۔ بتائیں، میں آپ کی کیا مدد کر سکتی ہوں؟»
    </div>

    <div class="info">
      <p>✨ <strong>مکمل AI خصوصیات:</strong> Assistant موڈ، Friend موڈ، Companion موڈ، اور Romantic (گرل فرینڈ) موڈ شامل ہیں۔</p>
      <p>📞 <strong>فون و ایپس کنٹرول:</strong> WhatsApp، کیمرہ، گیلری، یوٹیوب، ٹک ٹاک، ٹارچ، والیم، اور بیٹری اسٹیٹس کے لائیو فنکشنز۔</p>
      <p>🔔 <strong>بیک گراؤنڈ نوٹیفکیشن اسسٹنٹ:</strong> WhatsApp اور پیغامات کے نوٹیفکیشنز کا صوتی اعلان، نجی معلومات کا تحفظ۔</p>
      <p>🧠 <strong>محفوظ مقامی یادداشت:</strong> Room ڈیٹا بیس میں ترجیحات اور یادداشتیں محفوظ کرنے اور صاف کرنے کی مکمل سہولت۔</p>
    </div>
  </div>
</body>
</html>`);
});

server.listen(port, () => {
  console.log(`ZORIA dev server running on port ${port}`);
});
