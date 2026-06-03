<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Car Zisan - Rent A Car</title>
  <style>
    :root {
      --bg: #f4efe6;
      --ink: #1f2937;
      --accent: #c2410c;
      --accent-2: #9a3412;
      --card: #fffaf0;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
      color: var(--ink);
      background:
        radial-gradient(circle at 10% 0%, #fde68a 0%, transparent 32%),
        radial-gradient(circle at 90% 100%, #fed7aa 0%, transparent 28%),
        linear-gradient(160deg, #fef3c7 0%, var(--bg) 55%);
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
    }
    .wrap {
      width: min(880px, 100%);
      background: color-mix(in srgb, var(--card) 92%, white 8%);
      border: 1px solid #fed7aa;
      border-radius: 24px;
      padding: 40px 28px;
      box-shadow: 0 20px 50px rgba(0,0,0,0.08);
      text-align: center;
    }
    h1 {
      margin: 0;
      font-size: clamp(2rem, 6vw, 3.8rem);
      line-height: 1.05;
      letter-spacing: 0.4px;
    }
    p {
      margin: 16px auto;
      max-width: 62ch;
      font-size: clamp(1rem, 2.2vw, 1.2rem);
      line-height: 1.7;
    }
    .badge {
      display: inline-block;
      margin-top: 12px;
      padding: 10px 16px;
      border-radius: 999px;
      color: #fff;
      background: linear-gradient(135deg, var(--accent), var(--accent-2));
      font-weight: 700;
      letter-spacing: .4px;
    }
    .meta {
      margin-top: 18px;
      color: #374151;
      font-size: .95rem;
    }
  </style>
</head>
<body>
  <main class="wrap">
    <h1>Car Zisan</h1>
    <p>
      Welcome to your new car rental website. The domain is connected and this site is now live from
      <strong>/var/www/html/car.zisan.me</strong>.
    </p>
    <div class="badge">Deployment Complete</div>
    <div class="meta">Repository: git@github.com:rzzisan/rentacar.git</div>
  </main>
</body>
</html>
