package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

/**
 * Shared OOP domain vocabularies and choice builders for procedural OOP Blitz engines.
 */
final class OopBlitzDomains {

    record ClassDomain(
            String cls,
            String strField,
            String intField,
            String method,
            String val1,
            String val2) {
    }

    record HierarchyDomain(
            String parent,
            String child,
            String sibling,
            String method,
            String parentOut,
            String childOut,
            String siblingOut,
            String parentTag,
            String childTag) {
    }

    record TripleHierarchy(
            String grand,
            String parent,
            String child,
            String gTag,
            String pTag,
            String cTag) {
    }

    record UmlTuple(
            String left,
            String right,
            String member,
            String fieldName) {
    }

    record PackagePair(
            String pkgA,
            String pkgB,
            String parent,
            String child,
            String outsider,
            String member,
            int intValue) {
    }

    static final List<ClassDomain> CLASS_DOMAINS = List.of(
            new ClassDomain("BankAccount", "owner", "balance", "deposit", "Alice", "VIP"),
            new ClassDomain("LibraryBook", "title", "copies", "borrow", "CleanCode", "EIU"),
            new ClassDomain("OrderItem", "sku", "qty", "ship", "KB-01", "Priority"),
            new ClassDomain("Vehicle", "plate", "speed", "drive", "EIU-79", "Eco"),
            new ClassDomain("Patient", "name", "ward", "admit", "Minh", "ICU"),
            new ClassDomain("Student", "irn", "credits", "enroll", "2331", "Honors"),
            new ClassDomain("HotelRoom", "code", "floor", "book", "A204", "Suite"),
            new ClassDomain("Ticket", "seat", "price", "issue", "B12", "Early"),
            new ClassDomain("Beverage", "flavor", "shots", "brew", "Matcha", "Iced"),
            new ClassDomain("Sensor", "zone", "reading", "sample", "Lab3", "Alert"),
            new ClassDomain("Parcel", "tracking", "weight", "dispatch", "VN99", "Express"),
            new ClassDomain("Employee", "badge", "level", "promote", "ENG4", "Lead"),
            new ClassDomain("Flight", "callsign", "gate", "board", "VN210", "GateB"),
            new ClassDomain("Track", "genre", "duration", "play", "Jazz", "Live"),
            new ClassDomain("Workout", "focus", "reps", "log", "Core", "Timed"),
            new ClassDomain("ParkingSlot", "lot", "tier", "reserve", "West", "EV"),
            new ClassDomain("Dish", "course", "calories", "serve", "Ramen", "Chef"),
            new ClassDomain("InventoryItem", "bin", "stock", "restock", "Z14", "Fragile"));

    static final List<HierarchyDomain> HIERARCHIES = List.of(
            new HierarchyDomain("Animal", "Cat", "Dog", "speak", "...", "Meow", "Woof", "Anim", "Cat"),
            new HierarchyDomain("Animal", "Duck", "Frog", "sound", "None", "Quack", "Ribbit", "Base", "Duck"),
            new HierarchyDomain("Shape", "Circle", "Square", "kind", "Shape", "Circle", "Square", "S", "C"),
            new HierarchyDomain("Payment", "CardPay", "CashPay", "label", "Pay", "Card", "Cash", "Generic", "Card"),
            new HierarchyDomain("Notifier", "EmailSender", "SmsSender", "channel", "Msg", "Email", "SMS", "Base", "Mail"),
            new HierarchyDomain("Vehicle", "Car", "Bike", "mode", "Move", "Drive", "Pedal", "V", "C"),
            new HierarchyDomain("Account", "Savings", "Checking", "tier", "Acc", "Save", "Check", "Base", "Sav"),
            new HierarchyDomain("Employee", "Manager", "Intern", "role", "Staff", "Lead", "Trainee", "Emp", "Mgr"),
            new HierarchyDomain("Document", "Invoice", "Memo", "format", "Doc", "PDF", "TXT", "Doc", "Inv"),
            new HierarchyDomain("Parser", "JsonParser", "XmlParser", "type", "Raw", "JSON", "XML", "P", "J"),
            new HierarchyDomain("Storage", "CloudDisk", "LocalDisk", "mode", "Disk", "Cloud", "SSD", "Base", "Cloud"),
            new HierarchyDomain("Printer", "LaserPrinter", "InkPrinter", "tech", "Print", "Laser", "Ink", "Prn", "Lsr"),
            new HierarchyDomain("Logger", "FileLogger", "ConsoleLogger", "sink", "Log", "File", "Term", "L", "F"),
            new HierarchyDomain("Sensor", "TempSensor", "GasSensor", "metric", "Val", "Celsius", "PPM", "Sen", "Tmp"),
            new HierarchyDomain("Button", "SubmitBtn", "CancelBtn", "action", "Click", "Submit", "Cancel", "Btn", "Sub"),
            new HierarchyDomain("Filter", "BlurFilter", "SharpenFilter", "pass", "Fx", "Blur", "Sharp", "Flt", "Blr"));

    static final List<TripleHierarchy> TRIPLE_HIERARCHIES = List.of(
            new TripleHierarchy("Entity", "User", "Student", "E", "U", "S"),
            new TripleHierarchy("Node", "Widget", "Button", "N", "W", "B"),
            new TripleHierarchy("Device", "Computer", "Laptop", "D", "C", "L"),
            new TripleHierarchy("Vehicle", "MotorVehicle", "Sedan", "V", "M", "S"),
            new TripleHierarchy("Asset", "Account", "Savings", "A", "C", "V"),
            new TripleHierarchy("Media", "Audio", "Podcast", "M", "A", "P"),
            new TripleHierarchy("Token", "AuthToken", "JwtToken", "T", "A", "J"),
            new TripleHierarchy("Stream", "DataStream", "FileStream", "S", "D", "F"));

    static final List<UmlTuple> EXTENDS_TUPLES = List.of(
            new UmlTuple("Animal", "Dog", "bark", "breed"),
            new UmlTuple("Vehicle", "Car", "drive", "model"),
            new UmlTuple("Account", "SavingsAccount", "accrue", "rate"),
            new UmlTuple("Employee", "Developer", "code", "stack"),
            new UmlTuple("Shape", "Rectangle", "area", "width"),
            new UmlTuple("Document", "Report", "render", "title"),
            new UmlTuple("Exception", "GradingException", "detail", "code"),
            new UmlTuple("Device", "Tablet", "boot", "screen"),
            new UmlTuple("Drink", "Espresso", "brew", "roast"),
            new UmlTuple("Notification", "EmailAlert", "send", "subject"),
            new UmlTuple("Parser", "JavaParser", "parse", "tokens"),
            new UmlTuple("Widget", "TextBox", "draw", "text"),
            new UmlTuple("Course", "LabCourse", "grade", "room"),
            new UmlTuple("Ticket", "VipTicket", "scan", "lounge"));

    static final List<UmlTuple> IMPLEMENTS_TUPLES = List.of(
            new UmlTuple("Flyable", "Duck", "fly", "wings"),
            new UmlTuple("Payable", "Invoice", "pay", "amount"),
            new UmlTuple("Sortable", "ScoreRow", "compare", "score"),
            new UmlTuple("Printable", "Receipt", "print", "footer"),
            new UmlTuple("Authenticatable", "CampusUser", "verify", "token"),
            new UmlTuple("Exportable", "GradeSheet", "export", "csv"),
            new UmlTuple("Trackable", "Shipment", "locate", "gps"),
            new UmlTuple("Cacheable", "LabRubric", "key", "ttl"),
            new UmlTuple("Auditable", "Submission", "audit", "actor"),
            new UmlTuple("Schedulable", "ExamSlot", "schedule", "time"),
            new UmlTuple("Searchable", "CatalogItem", "match", "query"),
            new UmlTuple("Closeable", "WorkerClient", "close", "pid"),
            new UmlTuple("Gradable", "Challenge", "score", "weight"),
            new UmlTuple("Serializable", "Snapshot", "pack", "bytes"));

    static final List<UmlTuple> COMPOSITION_TUPLES = List.of(
            new UmlTuple("Car", "Engine", "ignite", "engine"),
            new UmlTuple("House", "Room", "clean", "mainRoom"),
            new UmlTuple("Order", "OrderLine", "subtotal", "primaryLine"),
            new UmlTuple("Exam", "Question", "score", "firstQuestion"),
            new UmlTuple("Laptop", "Battery", "charge", "battery"),
            new UmlTuple("Human", "Heart", "beat", "heart"),
            new UmlTuple("University", "Department", "budget", "coreDept"),
            new UmlTuple("Deck", "Card", "draw", "topCard"),
            new UmlTuple("Invoice", "TaxEntry", "rate", "tax"),
            new UmlTuple("Building", "Elevator", "move", "lift"),
            new UmlTuple("Playlist", "QueueNode", "next", "head"),
            new UmlTuple("GameMatch", "ScoreBoard", "tally", "board"),
            new UmlTuple("Submission", "CompileLog", "flush", "log"),
            new UmlTuple("Calendar", "DaySlot", "open", "slot"));

    static final List<UmlTuple> AGGREGATION_TUPLES = List.of(
            new UmlTuple("Team", "Player", "roster", "players"),
            new UmlTuple("Department", "Professor", "faculty", "staff"),
            new UmlTuple("Playlist", "Song", "tracks", "songs"),
            new UmlTuple("Classroom", "Desk", "layout", "desks"),
            new UmlTuple("Library", "Book", "catalog", "books"),
            new UmlTuple("Cart", "Product", "items", "products"),
            new UmlTuple("Club", "Member", "directory", "members"),
            new UmlTuple("Fleet", "Truck", "vehicles", "trucks"),
            new UmlTuple("Course", "Student", "roster", "students"),
            new UmlTuple("Project", "Contributor", "team", "contributors"),
            new UmlTuple("Gallery", "Photo", "album", "photos"),
            new UmlTuple("Menu", "Dish", "offers", "dishes"),
            new UmlTuple("Ward", "Nurse", "shift", "nurses"),
            new UmlTuple("Shelf", "Folder", "slots", "folders"));

    static final List<UmlTuple> ASSOCIATION_TUPLES = List.of(
            new UmlTuple("Student", "Course", "enroll", "enrolled"),
            new UmlTuple("Doctor", "Patient", "assign", "patient"),
            new UmlTuple("Pilot", "Aircraft", "fly", "assignedCraft"),
            new UmlTuple("Driver", "Route", "follow", "route"),
            new UmlTuple("Teacher", "Section", "teach", "section"),
            new UmlTuple("Author", "Publisher", "sign", "publisher"),
            new UmlTuple("Customer", "Coupon", "apply", "coupon"),
            new UmlTuple("Cashier", "Register", "operate", "register"),
            new UmlTuple("Player", "Guild", "join", "guild"),
            new UmlTuple("Tenant", "Landlord", "lease", "landlord"),
            new UmlTuple("Reviewer", "Paper", "review", "paper"),
            new UmlTuple("Member", "Locker", "claim", "locker"),
            new UmlTuple("Coach", "Athlete", "train", "athlete"),
            new UmlTuple("Courier", "Hub", "report", "homeHub"));

    static final List<UmlTuple> DEPENDENCY_TUPLES = List.of(
            new UmlTuple("OrderService", "PaymentGateway", "checkout", "gw"),
            new UmlTuple("ReportExporter", "CsvFormatter", "export", "fmt"),
            new UmlTuple("AuthController", "TokenVerifier", "login", "verifier"),
            new UmlTuple("GradeService", "ScoreCalculator", "compute", "calc"),
            new UmlTuple("Mailer", "TemplateEngine", "send", "engine"),
            new UmlTuple("LabImporter", "ZipReader", "load", "reader"),
            new UmlTuple("CheckoutFlow", "TaxPolicy", "quote", "policy"),
            new UmlTuple("RenderEngine", "FontMetrics", "measure", "metrics"),
            new UmlTuple("SearchService", "QueryParser", "find", "parser"),
            new UmlTuple("BackupJob", "FileCompressor", "archive", "zipper"),
            new UmlTuple("UploadHandler", "VirusScanner", "accept", "scanner"),
            new UmlTuple("QuizEngine", "RandomSeed", "shuffle", "seed"),
            new UmlTuple("ReceiptPrinter", "CurrencyHelper", "format", "helper"),
            new UmlTuple("AuditLogger", "ClockSource", "record", "clock"));

    static final List<PackagePair> PACKAGE_PAIRS = List.of(
            new PackagePair("core", "app", "Parent", "Child", "Runner", "code", 7),
            new PackagePair("model", "service", "BaseEntity", "OrderEntity", "Worker", "secretId", 14),
            new PackagePair("billing", "ui", "Account", "VipAccount", "Dashboard", "limit", 25),
            new PackagePair("domain", "api", "Record", "LabRecord", "Handler", "version", 3),
            new PackagePair("security", "web", "Principal", "StudentAuth", "Filter", "rank", 9),
            new PackagePair("grading", "export", "Metric", "ScoreMetric", "Exporter", "weight", 5),
            new PackagePair("school", "portal", "Person", "Lecturer", "Portal", "pin", 42),
            new PackagePair("storage", "cli", "Bucket", "LocalBucket", "CliTool", "quota", 80),
            new PackagePair("network", "client", "SocketBase", "TlsSocket", "Client", "timeout", 30),
            new PackagePair("inventory", "shop", "Item", "BundleItem", "CartView", "margin", 12),
            new PackagePair("transit", "map", "Node", "StationNode", "MapView", "zone", 4),
            new PackagePair("media", "player", "Clip", "VideoClip", "Screen", "bitrate", 64));

    private static final List<String> SAFE_DISTRACTOR_FALLBACKS = List.of(
            "Compile error",
            "null",
            "0",
            "1",
            "-1",
            "Runtime exception",
            "false",
            "true");

    private OopBlitzDomains() {
    }

    static ClassDomain pickDomain(RandomGenerator rng) {
        return CLASS_DOMAINS.get(rng.nextInt(CLASS_DOMAINS.size()));
    }

    static HierarchyDomain pickHierarchy(RandomGenerator rng) {
        return HIERARCHIES.get(rng.nextInt(HIERARCHIES.size()));
    }

    static TripleHierarchy pickTriple(RandomGenerator rng) {
        return TRIPLE_HIERARCHIES.get(rng.nextInt(TRIPLE_HIERARCHIES.size()));
    }

    static UmlTuple pickUml(List<UmlTuple> pool, RandomGenerator rng) {
        return pool.get(rng.nextInt(pool.size()));
    }

    static PackagePair pickPackagePair(RandomGenerator rng) {
        return PACKAGE_PAIRS.get(rng.nextInt(PACKAGE_PAIRS.size()));
    }

    static StoredQuestion choiceQuestion(
            String key,
            String kind,
            String difficulty,
            int timeLimitSec,
            String prompt,
            List<String> codeLines,
            String correctOption,
            List<String> distractors,
            String explanation,
            RandomGenerator rng) {
        record IndexedChoice(String label, boolean correct) {
        }

        Set<String> unique = new LinkedHashSet<>();
        unique.add(correctOption);
        for (String candidate : distractors) {
            if (unique.size() >= 4) {
                break;
            }
            if (candidate != null && !candidate.isBlank()) {
                unique.add(candidate);
            }
        }
        for (String fallback : SAFE_DISTRACTOR_FALLBACKS) {
            if (unique.size() >= 4) {
                break;
            }
            unique.add(fallback);
        }

        List<IndexedChoice> items = new ArrayList<>(4);
        boolean first = true;
        for (String label : unique) {
            items.add(new IndexedChoice(label, first));
            first = false;
        }
        shuffle(items, rng);

        List<String> options = new ArrayList<>(4);
        int correctIdx = 0;
        for (int i = 0; i < items.size(); i++) {
            options.add(items.get(i).label());
            if (items.get(i).correct()) {
                correctIdx = i;
            }
        }
        return new StoredQuestion(
                key,
                kind,
                difficulty,
                timeLimitSec,
                prompt,
                List.copyOf(codeLines),
                List.copyOf(options),
                correctIdx,
                explanation);
    }

    static <T> void shuffle(List<T> list, RandomGenerator rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
