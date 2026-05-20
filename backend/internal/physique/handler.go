package physique

import (
	"context"
	"encoding/json"
	"net/http"
	"time"

	"ascend-backend/internal/middleware"
	"ascend-backend/pkg/response"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Handler struct{ db *pgxpool.Pool }

func NewHandler(db *pgxpool.Pool) *Handler { return &Handler{db: db} }

type SaveRequest struct {
	Age            int     `json:"age"`
	Sex            string  `json:"sex"`
	HeightCM       float64 `json:"height_cm"`
	WeightKG       float64 `json:"weight_kg"`
	TargetWeightKG float64 `json:"target_weight_kg"`
	BodyGoal       string  `json:"body_goal"`
	ActivityLevel  string  `json:"activity_level"`
	FitnessLevel   string  `json:"fitness_level"`
}

func (h *Handler) Save(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var req SaveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid body")
		return
	}

	if req.Age < 10 || req.Age > 100 {
		response.Error(w, http.StatusBadRequest, "age must be between 10 and 100")
		return
	}
	if req.HeightCM < 100 || req.HeightCM > 250 {
		response.Error(w, http.StatusBadRequest, "height_cm must be between 100 and 250")
		return
	}
	if req.WeightKG < 20 || req.WeightKG > 300 {
		response.Error(w, http.StatusBadRequest, "weight_kg must be between 20 and 300")
		return
	}

	metrics := Compute(req.WeightKG, req.HeightCM, req.Age, req.Sex,
		req.ActivityLevel, req.BodyGoal)

	_, err := h.db.Exec(r.Context(),
		`INSERT INTO physique_profiles
		   (user_id, age, sex, height_cm, weight_kg, target_weight_kg,
		    body_goal, activity_level, fitness_level,
		    bmi, bmr, tdee, computed_at, updated_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$13)
		 ON CONFLICT (user_id) DO UPDATE SET
		   age=$2, sex=$3, height_cm=$4, weight_kg=$5,
		   target_weight_kg=$6, body_goal=$7, activity_level=$8,
		   fitness_level=$9, bmi=$10, bmr=$11, tdee=$12,
		   updated_at=$13`,
		userID,
		req.Age, req.Sex, req.HeightCM, req.WeightKG, req.TargetWeightKG,
		req.BodyGoal, req.ActivityLevel, req.FitnessLevel,
		metrics.BMI, metrics.BMR, metrics.TDEE, time.Now(),
	)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to save physique profile")
		return
	}

	response.JSON(w, http.StatusOK, map[string]any{
		"bmi":           metrics.BMI,
		"bmi_category":  string(metrics.BMICategory),
		"bmr":           metrics.BMR,
		"tdee":          metrics.TDEE,
		"goal_calories": metrics.GoalCalories,
		"message":       "physique profile saved",
	})
}

func (h *Handler) Get(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	type Profile struct {
		Age            int     `json:"age"`
		Sex            string  `json:"sex"`
		HeightCM       float64 `json:"height_cm"`
		WeightKG       float64 `json:"weight_kg"`
		TargetWeightKG float64 `json:"target_weight_kg"`
		BodyGoal       string  `json:"body_goal"`
		ActivityLevel  string  `json:"activity_level"`
		FitnessLevel   string  `json:"fitness_level"`
		BMI            float64 `json:"bmi"`
		BMICategory    string  `json:"bmi_category"`
		BMR            int     `json:"bmr"`
		TDEE           int     `json:"tdee"`
		GoalCalories   int     `json:"goal_calories"`
	}

	var p Profile
	err := h.db.QueryRow(r.Context(),
		`SELECT age, sex, height_cm, weight_kg,
		        COALESCE(target_weight_kg, weight_kg),
		        body_goal, activity_level, fitness_level, bmi, bmr, tdee
		 FROM physique_profiles WHERE user_id=$1`,
		userID,
	).Scan(
		&p.Age, &p.Sex, &p.HeightCM, &p.WeightKG, &p.TargetWeightKG,
		&p.BodyGoal, &p.ActivityLevel, &p.FitnessLevel,
		&p.BMI, &p.BMR, &p.TDEE,
	)
	if err != nil {
		response.Error(w, http.StatusNotFound, "physique profile not set up")
		return
	}

	p.BMICategory = string(ClassifyBMI(p.BMI))
	p.GoalCalories = GoalCalories(p.TDEE, p.BodyGoal)
	response.JSON(w, http.StatusOK, p)
}

func GetProfile(ctx context.Context, db *pgxpool.Pool, userID string) (*SaveRequest, *PhysiqueMetrics, error) {
	var req SaveRequest
	var bmi float64
	var bmr, tdee int
	err := db.QueryRow(ctx,
		`SELECT age, sex, height_cm, weight_kg,
		        COALESCE(target_weight_kg, weight_kg),
		        body_goal, activity_level, fitness_level, bmi, bmr, tdee
		 FROM physique_profiles WHERE user_id=$1`,
		userID,
	).Scan(
		&req.Age, &req.Sex, &req.HeightCM, &req.WeightKG, &req.TargetWeightKG,
		&req.BodyGoal, &req.ActivityLevel, &req.FitnessLevel, &bmi, &bmr, &tdee,
	)
	if err != nil {
		return nil, nil, err
	}
	metrics := &PhysiqueMetrics{
		BMI: bmi, BMICategory: ClassifyBMI(bmi),
		BMR: bmr, TDEE: tdee,
		GoalCalories: GoalCalories(tdee, req.BodyGoal),
	}
	return &req, metrics, nil
}
