package interests

// ── internal/interests/categories.go ─────────────────────────────────────
// The master taxonomy of categories and subcategories.
// This is the source of truth — Android reads this from /interests/categories.

// Category is a top-level area of self-improvement.
type Category struct {
	ID            string        `json:"id"`
	Name          string        `json:"name"`
	Icon          string        `json:"icon"` // Material icon name for Android
	Description   string        `json:"description"`
	Color         string        `json:"color"` // hex for UI theming
	Subcategories []Subcategory `json:"subcategories"`
}

// Subcategory is a specific focus within a category.
type Subcategory struct {
	ID          string `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
	QuestHints  string `json:"quest_hints"` // injected into RAG prompt
}

// AllCategories is the full taxonomy. Add new entries here — no DB change needed.
var AllCategories = []Category{
	{
		ID:          "technology",
		Name:        "Technology",
		Icon:        "computer",
		Description: "Build technical skills and stay ahead in the digital world",
		Color:       "#7C3AED",
		Subcategories: []Subcategory{
			{
				ID:          "software",
				Name:        "Software Development",
				Description: "Coding, algorithms, system design",
				QuestHints:  "Generate quests about coding practice, open-source contributions, leetcode problems, building projects, learning new frameworks, reading technical documentation",
			},
			{
				ID:          "hardware",
				Name:        "Hardware & Electronics",
				Description: "Circuits, embedded systems, IoT",
				QuestHints:  "Generate quests about circuit building, Arduino/Raspberry Pi projects, soldering practice, reading datasheets, designing PCBs",
			},
			{
				ID:          "ai_ml",
				Name:        "AI & Machine Learning",
				Description: "Models, data science, research",
				QuestHints:  "Generate quests about implementing ML models, reading AI papers, Kaggle competitions, experimenting with neural networks, learning math for ML",
			},
			{
				ID:          "cybersecurity",
				Name:        "Cybersecurity",
				Description: "Ethical hacking, defence, networking",
				QuestHints:  "Generate quests about CTF challenges, learning networking fundamentals, studying cryptography, setting up home labs, reading CVE writeups",
			},
			{
				ID:          "devops",
				Name:        "DevOps & Cloud",
				Description: "Infrastructure, CI/CD, cloud platforms",
				QuestHints:  "Generate quests about setting up Docker containers, writing Terraform, configuring GitHub Actions, studying for cloud certifications, optimizing deployment pipelines",
			},
		},
	},
	{
		ID:          "physical",
		Name:        "Physical",
		Icon:        "fitness_center",
		Description: "Build your body, health, and physical endurance",
		Color:       "#EF4444",
		Subcategories: []Subcategory{
			{
				ID:          "strength",
				Name:        "Strength Training",
				Description: "Weightlifting, resistance, muscle building",
				QuestHints:  "Generate quests about progressive overload training, compound lifts (squat/deadlift/bench), home workouts, tracking PRs, rest and recovery protocols",
			},
			{
				ID:          "cardio",
				Name:        "Cardio & Endurance",
				Description: "Running, cycling, stamina",
				QuestHints:  "Generate quests about interval training, zone 2 cardio, building a running base, tracking VO2 max, progressive distance milestones",
			},
			{
				ID:          "flexibility",
				Name:        "Flexibility & Mobility",
				Description: "Yoga, stretching, joint health",
				QuestHints:  "Generate quests about daily stretching routines, yoga flows, hip mobility drills, thoracic spine work, foam rolling protocols",
			},
			{
				ID:          "nutrition",
				Name:        "Nutrition & Diet",
				Description: "Eating habits, tracking, meal prep",
				QuestHints:  "Generate quests about meal prepping, tracking macros, building healthy eating habits, trying new whole foods, reducing processed foods",
			},
			{
				ID:          "sports",
				Name:        "Sports & Athletics",
				Description: "Specific sport skills and drills",
				QuestHints:  "Generate quests about skill drills for chosen sport, joining local teams or clubs, tracking match performance, studying sport tactics",
			},
		},
	},
	{
		ID:          "mental",
		Name:        "Mental",
		Icon:        "psychology",
		Description: "Sharpen your mind, emotional intelligence, and focus",
		Color:       "#06B6D4",
		Subcategories: []Subcategory{
			{
				ID:          "mindfulness",
				Name:        "Mindfulness & Meditation",
				Description: "Focus, calm, present awareness",
				QuestHints:  "Generate quests about daily meditation practice, breath work, body scan exercises, reducing screen time before bed, mindful eating",
			},
			{
				ID:          "learning",
				Name:        "Continuous Learning",
				Description: "Books, courses, new subjects",
				QuestHints:  "Generate quests about reading non-fiction books, completing online courses, learning a new topic daily, taking structured notes, teaching what you learn",
			},
			{
				ID:          "emotional",
				Name:        "Emotional Intelligence",
				Description: "Self-awareness, empathy, regulation",
				QuestHints:  "Generate quests about journaling emotions, practising empathy in conversations, identifying cognitive biases, therapy exercises, conflict resolution practice",
			},
			{
				ID:          "productivity",
				Name:        "Productivity & Focus",
				Description: "Deep work, time management, habits",
				QuestHints:  "Generate quests about implementing deep work blocks, time-boxing tasks, building a second brain, eliminating distractions, reviewing weekly goals",
			},
			{
				ID:          "creativity",
				Name:        "Creativity",
				Description: "Art, writing, music, design",
				QuestHints:  "Generate quests about daily creative exercises, finishing a creative project, experimenting with new mediums, studying the work of masters, creative constraints challenges",
			},
		},
	},
	{
		ID:          "social",
		Name:        "Social & Communication",
		Icon:        "group",
		Description: "Build relationships, influence, and communication skills",
		Color:       "#D4AF37",
		Subcategories: []Subcategory{
			{
				ID:          "public_speaking",
				Name:        "Public Speaking",
				Description: "Presentations, confidence, clarity",
				QuestHints:  "Generate quests about recording yourself speaking, joining Toastmasters, preparing a talk, studying rhetoric, storytelling practice",
			},
			{
				ID:          "networking",
				Name:        "Networking & Relationships",
				Description: "Meeting people, maintaining connections",
				QuestHints:  "Generate quests about reaching out to one new contact, attending a meetup, following up with old connections, contributing to online communities",
			},
			{
				ID:          "writing",
				Name:        "Writing & Storytelling",
				Description: "Essays, blogs, persuasion",
				QuestHints:  "Generate quests about writing daily, publishing a blog post, practising persuasive writing, studying great writers, getting feedback on your work",
			},
		},
	},
	{
		ID:          "finance",
		Name:        "Finance & Career",
		Icon:        "trending_up",
		Description: "Build wealth, career capital, and financial freedom",
		Color:       "#10B981",
		Subcategories: []Subcategory{
			{
				ID:          "investing",
				Name:        "Investing",
				Description: "Stocks, crypto, passive income",
				QuestHints:  "Generate quests about researching investment options, reading financial statements, tracking portfolio, studying valuation methods, building an emergency fund",
			},
			{
				ID:          "career",
				Name:        "Career Growth",
				Description: "Skills, promotions, side projects",
				QuestHints:  "Generate quests about updating resume, applying to stretch roles, building a portfolio, getting mentorship, contributing to high-visibility projects",
			},
			{
				ID:          "entrepreneurship",
				Name:        "Entrepreneurship",
				Description: "Starting and scaling ventures",
				QuestHints:  "Generate quests about validating a business idea, talking to potential customers, building an MVP, studying successful founders, tracking key business metrics",
			},
		},
	},
}
