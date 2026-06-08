# লেআউট সিস্টেম ডকুমেন্টেশন

## 概要
এই অ্যাপ্লিকেশনে একটি সামঞ্জস্যপূর্ণ লেআউট সিস্টেম বাস্তবায়ন করা হয়েছে যা সমস্ত পৃষ্ঠা জুড়ে একটি একীভূত ডিজাইন নিশ্চিত করে।

## ফাইল কাঠামো

### CSS ফাইল
- `public/assets/css/layout.css` - সম্পূর্ণ লেআউট এবং থিমিং সিস্টেম

### কম্পোনেন্ট
- `includes/header.php` - হেডার কম্পোনেন্ট (সকল পৃষ্ঠায় অন্তর্ভুক্ত)
- `includes/sidebar.php` - সাইডবার নেভিগেশন (ভূমিকা-ভিত্তিক মেনু)

## রঙ স্কিম

```css
--primary: #667eea          /* প্রাথমিক নীল */
--primary-dark: #5568d3     /* গাঢ় নীল */
--secondary: #764ba2        /* বেগুনি */
--success: #4ade80          /* সবুজ */
--danger: #ff6b6b           /* লাল */
--warning: #fbbf24          /* হলুদ */
--info: #60a5fa             /* হালকা নীল */
--light: #f5f7fa            /* হালকা ধূসর */
--dark: #333                /* গাঢ় */
```

## পৃষ্ঠা কাঠামো

প্রতিটি পৃষ্ঠা নিম্নলিখিত কাঠামো অনুসরণ করে:

```html
<div class="app-layout">
    <!-- Header Component -->
    <?php include '../includes/header.php'; ?>
    
    <div class="app-content">
        <!-- Sidebar Component -->
        <?php include '../includes/sidebar.php'; ?>
        
        <!-- Main Content Area -->
        <main class="main-content">
            <!-- Page Header -->
            <div class="page-header">
                <h1>পৃষ্ঠা শিরোনাম</h1>
                <div class="breadcrumb">ব্রেডক্রাম্ব পথ</div>
            </div>
            
            <!-- Page Content -->
            <div class="content-card">
                <!-- Content goes here -->
            </div>
        </main>
    </div>
</div>
```

## নতুন পৃষ্ঠা তৈরি করা

### ধাপ ১: নতুন ফাইল তৈরি করুন

সংশ্লিষ্ট ভূমিকা ফোল্ডারে (admin/, employee/, customer/) নতুন PHP ফাইল তৈরি করুন।

### ধাপ ২: প্রয়োজনীয় ফাইল অন্তর্ভুক্ত করুন

```php
<?php
require_once '../config/config.php';
require_once '../config/Database.php';
require_once '../includes/User.php';

// ... অন্যান্য প্রয়োজনীয় ফাইল

$database = new Database();
$conn = $database->connect();
$user = new User($conn);

// ভূমিকা পরীক্ষা করুন
if (!$user->isLoggedIn() || !$user->hasRole('admin')) {
    header('Location: ../index.php');
    exit();
}

$current_user = $user->getCurrentUser();
?>
```

### ধাপ ৩: HTML এবং CSS যোগ করুন

```html
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>পৃষ্ঠা শিরোনাম - <?php echo APP_NAME; ?></title>
    <link rel="stylesheet" href="../public/assets/css/layout.css">
</head>
<body>
    <div class="app-layout">
        <?php include '../includes/header.php'; ?>
        
        <div class="app-content">
            <?php include '../includes/sidebar.php'; ?>
            
            <main class="main-content">
                <!-- Page content -->
            </main>
        </div>
    </div>
    
    <script src="../public/assets/js/main.js"></script>
</body>
</html>
```

## সাইডবার মেনু যোগ করা

সাইডবার স্বয়ংক্রিয়ভাবে ভূমিকার উপর ভিত্তি করে মেনু প্রদর্শন করে। নতুন মেনু আইটেম যোগ করতে `includes/sidebar.php` সম্পাদনা করুন।

### ফরম্যাট:

#### সাধারণ লিংক
```php
<li class="menu-item">
    <a href="page.php" class="menu-link">
        <span class="menu-icon">📊</span>
        <span class="menu-label">মেনু লেবেল</span>
    </a>
</li>
```

#### সাব-মেনু সহ গ্রুপ
```php
<li class="menu-item has-submenu">
    <a href="#" class="menu-link">
        <span class="menu-icon">🚗</span>
        <span class="menu-label">প্যারেন্ট মেনু</span>
        <span class="menu-toggle">▶</span>
    </a>
    <ul class="submenu">
        <li class="submenu-item">
            <a href="page1.php" class="submenu-link">সাব মেনু ১</a>
        </li>
        <li class="submenu-item">
            <a href="page2.php" class="submenu-link">সাব মেনু ২</a>
        </li>
    </ul>
</li>
```

## কমপোনেন্ট এবং ক্লাস

### পৃষ্ঠা হেডার
```html
<div class="page-header">
    <div>
        <h1>শিরোনাম</h1>
        <div class="breadcrumb">ব্রেডক্রাম্ব পথ</div>
    </div>
    <div>অতিরিক্ত উপাদান</div>
</div>
```

### কন্টেন্ট কার্ড
```html
<div class="content-card">
    <h2>কার্ড শিরোনাম</h2>
    <!-- কন্টেন্ট -->
</div>
```

### বোতাম
```html
<a href="#" class="btn">প্রাথমিক বোতাম</a>
<a href="#" class="btn btn-secondary">মাধ্যমিক বোতাম</a>
<a href="#" class="btn btn-success">সফলতা বোতাম</a>
<a href="#" class="btn btn-danger">বিপদ বোতাম</a>
```

### সতর্কতা বার্তা
```html
<div class="alert alert-success">✓ সফলতার বার্তা</div>
<div class="alert alert-error">✗ ত্রুটি বার্তা</div>
<div class="alert alert-warning">⚠ সতর্কতা বার্তা</div>
<div class="alert alert-info">ℹ তথ্য বার্তা</div>
```

### ব্যাজ
```html
<span class="badge badge-success">সফলতা</span>
<span class="badge badge-danger">বিপদ</span>
<span class="badge badge-warning">সতর্কতা</span>
<span class="badge badge-info">তথ্য</span>
```

### টেবিল
```html
<table class="table">
    <thead>
        <tr>
            <th>কলাম ১</th>
            <th>কলাম ২</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>ডেটা</td>
            <td>ডেটা</td>
        </tr>
    </tbody>
</table>
```

### গ্রিড লেআউট
```html
<!-- 2 কলাম গ্রিড -->
<div class="grid grid-2">
    <div class="card">...</div>
    <div class="card">...</div>
</div>

<!-- 3 কলাম গ্রিড -->
<div class="grid grid-3">
    <div class="card">...</div>
    <div class="card">...</div>
</div>

<!-- 4 কলাম গ্রিড -->
<div class="grid grid-4">
    <div class="card">...</div>
    <div class="card">...</div>
</div>
```

## ফর্ম উপাদান

### একক ফর্ম গ্রুপ
```html
<div class="form-group">
    <label for="field">লেবেল:</label>
    <input type="text" id="field" name="field" required>
</div>
```

### ফর্ম সারি (২ কলাম)
```html
<div class="form-row">
    <div class="form-group">
        <label for="field1">লেবেল ১:</label>
        <input type="text" id="field1" name="field1">
    </div>
    <div class="form-group">
        <label for="field2">লেবেল ২:</label>
        <input type="text" id="field2" name="field2">
    </div>
</div>
```

### পূর্ণ প্রস্থ ফর্ম সারি
```html
<div class="form-row full">
    <div class="form-group">
        <label for="field">লেবেল:</label>
        <textarea id="field" name="field" rows="5"></textarea>
    </div>
</div>
```

## রেসপন্সিভ ডিজাইন

লেআউট সিস্টেম সম্পূর্ণরূপে রেসপন্সিভ এবং মোবাইল ডিভাইসে স্বয়ংক্রিয়ভাবে সামঞ্জস্য করে:

- **ডেস্কটপ (768px এবং উপরে):** পূর্ণ লেআউট সহ সাইডবার
- **ট্যাবলেট এবং মোবাইল (768px এর নিচে):** সাইডবার স্থিতিশীল হয় এবং টগল বোতাম প্রদর্শিত হয়

## JavaScript কার্যকারিতা

### সাইডবার টগল
সাইডবার টগল স্বয়ংক্রিয়ভাবে header.php থেকে সক্ষম করা হয়।

### সাব-মেনু টগল
সাব-মেনু স্বয়ংক্রিয়ভাবে sidebar.php থেকে সক্ষম করা হয়।

### স্থানীয় স্টোরেজ
সাইডবার অবস্থা localStorage-এ সংরক্ষিত থাকে এবং পৃষ্ঠা পুনরায় লোড করার পরে পুনরুদ্ধার করা হয়।

## রক্ষণাবেক্ষণ

### নতুন রঙ যোগ করা
`layout.css` এর `:root` সেকশনে নতুন রঙ ভেরিয়েবল যোগ করুন।

### নতুন কম্পোনেন্ট তৈরি করা
`layout.css`-এ নতুন ক্লাস তৈরি করুন এবং সামঞ্জস্যপূর্ণ রঙ এবং spacing ব্যবহার করুন।

### মেনু সংগঠন
মেনু আইটেমগুলি সাংগঠনিক রাখতে, `includes/sidebar.php`-এ `menu-section-title` ব্যবহার করুন।

## আপডেট হিস্টরি

### ২০২৬-০৬-০৪
- লেআউট সিস্টেম সম্পূর্ণভাবে পুনর্নির্মাণ করা হয়েছে
- হেডার এবং সাইডবার কম্পোনেন্ট তৈরি করা হয়েছে
- সমস্ত ড্যাশবোর্ড পৃষ্ঠা আপডেট করা হয়েছে
- গাড়ি ম্যানেজমেন্ট পৃষ্ঠা নতুন লেআউট দিয়ে আপডেট করা হয়েছে
