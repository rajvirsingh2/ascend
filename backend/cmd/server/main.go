package main

import (
	"fmt"
	"net/http"
)

func main() {

	fmt.Println("Ascend backend starting on :8080")

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintln(w, "Ascend Go backend running")
	})

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintln(w, `{"status":"ok","service":"backend"}`)
	})

	http.ListenAndServe(":8080", nil)
}