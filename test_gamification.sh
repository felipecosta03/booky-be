#!/bin/bash

# Test script for gamification functionality
# This script tests creating a community and verifying gamification points

BASE_URL="http://localhost:8080"
EMAIL="juan.perez@example.com"
PASSWORD="password123"

echo "🎮 Testing Gamification System..."
echo "=================================="

# 1. Login
echo "📝 Step 1: Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/sign-in" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

if [[ $? -ne 0 ]]; then
  echo "❌ Login failed - is the server running?"
  exit 1
fi

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.user.id')

if [[ "$TOKEN" == "null" || "$USER_ID" == "null" ]]; then
  echo "❌ Login failed - check credentials"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Login successful - User ID: $USER_ID"

# 2. Get initial gamification profile
echo "📊 Step 2: Getting initial gamification profile..."
INITIAL_PROFILE=$(curl -s -X GET "$BASE_URL/gamification/profile/$USER_ID" \
  -H "Authorization: Bearer $TOKEN")

if [[ $? -ne 0 ]]; then
  echo "❌ Failed to get gamification profile"
  exit 1
fi

INITIAL_POINTS=$(echo "$INITIAL_PROFILE" | jq -r '.totalPoints')
INITIAL_COMMUNITIES=$(echo "$INITIAL_PROFILE" | jq -r '.communitiesCreated')

echo "✅ Initial profile retrieved:"
echo "   Points: $INITIAL_POINTS"
echo "   Communities created: $INITIAL_COMMUNITIES"

# 3. Create a community
echo "🏘️ Step 3: Creating a test community..."
TIMESTAMP=$(date +%s)
COMMUNITY_RESPONSE=$(curl -s -X POST "$BASE_URL/communities" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"name\":\"Gamification Test $TIMESTAMP\",\"description\":\"Testing gamification points\"}")

if [[ $? -ne 0 ]]; then
  echo "❌ Failed to create community"
  exit 1
fi

COMMUNITY_ID=$(echo "$COMMUNITY_RESPONSE" | jq -r '.id')

if [[ "$COMMUNITY_ID" == "null" ]]; then
  echo "❌ Community creation failed"
  echo "Response: $COMMUNITY_RESPONSE"
  exit 1
fi

echo "✅ Community created - ID: $COMMUNITY_ID"

# 4. Wait a moment for gamification to process
echo "⏱️ Step 4: Waiting for gamification processing..."
sleep 2

# 5. Check updated gamification profile
echo "🎯 Step 5: Checking updated gamification profile..."
UPDATED_PROFILE=$(curl -s -X GET "$BASE_URL/gamification/profile/$USER_ID" \
  -H "Authorization: Bearer $TOKEN")

if [[ $? -ne 0 ]]; then
  echo "❌ Failed to get updated gamification profile"
  exit 1
fi

UPDATED_POINTS=$(echo "$UPDATED_PROFILE" | jq -r '.totalPoints')
UPDATED_COMMUNITIES=$(echo "$UPDATED_PROFILE" | jq -r '.communitiesCreated')

echo "✅ Updated profile retrieved:"
echo "   Points: $INITIAL_POINTS → $UPDATED_POINTS"
echo "   Communities created: $INITIAL_COMMUNITIES → $UPDATED_COMMUNITIES"

# 6. Verify the changes
echo "🔍 Step 6: Verifying gamification results..."

POINTS_DIFF=$((UPDATED_POINTS - INITIAL_POINTS))
COMMUNITIES_DIFF=$((UPDATED_COMMUNITIES - INITIAL_COMMUNITIES))

if [[ $POINTS_DIFF -eq 100 ]]; then
  echo "✅ Points increased correctly (+$POINTS_DIFF points)"
else
  echo "❌ Points did not increase correctly. Expected +100, got +$POINTS_DIFF"
fi

if [[ $COMMUNITIES_DIFF -eq 1 ]]; then
  echo "✅ Community counter increased correctly (+$COMMUNITIES_DIFF)"
else
  echo "❌ Community counter did not increase correctly. Expected +1, got +$COMMUNITIES_DIFF"
fi

# 7. Check for achievements
echo "🏆 Step 7: Checking for new achievements..."
ACHIEVEMENTS=$(curl -s -X GET "$BASE_URL/gamification/achievements/$USER_ID/unnotified" \
  -H "Authorization: Bearer $TOKEN")

if [[ $? -eq 0 ]]; then
  ACHIEVEMENT_COUNT=$(echo "$ACHIEVEMENTS" | jq '. | length')
  if [[ $ACHIEVEMENT_COUNT -gt 0 ]]; then
    echo "🎉 New achievements earned: $ACHIEVEMENT_COUNT"
    echo "$ACHIEVEMENTS" | jq -r '.[] | "   🏆 " + .achievement.name + ": " + .achievement.description'
  else
    echo "ℹ️ No new achievements earned"
  fi
fi

# 8. Summary
echo ""
echo "📋 GAMIFICATION TEST SUMMARY"
echo "=================================="

if [[ $POINTS_DIFF -eq 100 && $COMMUNITIES_DIFF -eq 1 ]]; then
  echo "🎉 ✅ GAMIFICATION IS WORKING CORRECTLY!"
  echo "   - Community creation awards 100 points ✅"
  echo "   - Community counter increments ✅"
  echo "   - Profile updates properly ✅"
else
  echo "❌ GAMIFICATION HAS ISSUES:"
  if [[ $POINTS_DIFF -ne 100 ]]; then
    echo "   - Points not awarded correctly (expected +100, got +$POINTS_DIFF) ❌"
  fi
  if [[ $COMMUNITIES_DIFF -ne 1 ]]; then
    echo "   - Community counter not incremented ❌"
  fi
fi

echo ""
echo "💡 Next steps to test:"
echo "   - Create a post (+15 points)"
echo "   - Comment on a post (+10 points)"
echo "   - Add/read books (+10/+25 points)"
echo "   - Join/create reading clubs (+25/+75 points)"
echo ""
echo "🎮 Gamification test completed!"
