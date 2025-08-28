// ===== CONFIGURACIÓN Y VARIABLES GLOBALES =====
const API_BASE_URL = 'http://localhost:8080';
let currentUser = null;
let authToken = null;
let allCommunities = [];
let allBooks = [];
let followingUsers = [];
let followers = [];

// ===== PERSISTENCIA DE SESIÓN =====
function saveToLocalStorage() {
    if (authToken && currentUser) {
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
    }
}

function loadFromLocalStorage() {
    const savedToken = localStorage.getItem('authToken');
    const savedUser = localStorage.getItem('currentUser');

    if (savedToken && savedUser) {
        authToken = savedToken;
        currentUser = JSON.parse(savedUser);
        return true;
    }
    return false;
}

function clearLocalStorage() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
}

// ===== UTILIDADES PRINCIPALES =====
class BookyAPI {
    static async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        if (authToken) {
            config.headers['Authorization'] = `Bearer ${authToken}`;
        }

        try {
            showLoading(true);
            console.log('Making API request:', {
                endpoint,
                method: config.method || 'GET',
                hasAuth: !!authToken,
                authToken: authToken ? `${authToken.substring(0, 20)}...` : null,
                authHeader: config.headers['Authorization'] ? `${config.headers['Authorization'].substring(0, 30)}...` : null,
                body: config.body
            });

            const response = await fetch(url, config);
            console.log('API response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('API Error Response:', errorText);

                if (response.status === 403) {
                    console.error('🚫 403 Forbidden - Auth problem:', {
                        endpoint,
                        hasToken: !!authToken,
                        tokenLength: authToken ? authToken.length : 0,
                        currentUser: currentUser?.id || 'null'
                    });
                }

                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const data = await response.json();
            console.log('API success response:', data);
            return data;
        } catch (error) {
            console.error('API Error:', error);
            showToast(`Error: ${error.message}`, 'error');
            throw error;
        } finally {
            showLoading(false);
        }
    }

    static async requestNoJson(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        if (authToken) {
            config.headers['Authorization'] = `Bearer ${authToken}`;
        }

        try {
            showLoading(true);
            console.log('Making API request (no JSON expected):', {
                endpoint,
                method: config.method || 'GET',
                hasAuth: !!authToken,
                body: config.body
            });

            const response = await fetch(url, config);
            console.log('API response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('API Error Response:', errorText);

                if (response.status === 403) {
                    console.error('🚫 403 Forbidden - Auth problem:', {
                        endpoint,
                        hasToken: !!authToken,
                        tokenLength: authToken ? authToken.length : 0,
                        currentUser: currentUser?.id || 'null'
                    });
                }

                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            console.log('API success response (no content expected)');
            return response; // Return response object instead of parsing JSON
        } catch (error) {
            console.error('API Error:', error);
            showToast(`Error: ${error.message}`, 'error');
            throw error;
        } finally {
            showLoading(false);
        }
    }

    // ===== AUTHENTICATION =====
    static async login(email, password) {
        return await this.request('/sign-in', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    }

    // ===== GAMIFICATION =====
    static async getGamificationProfile(userId) {
        return await this.request(`/gamification/profile/${userId}`);
    }

    static async getUserAchievements(userId) {
        return await this.request(`/gamification/achievements/${userId}`);
    }

    // ===== BOOKS =====
    static async getUserBooks(userId, options = {}) {
        let endpoint = `/books/library/${userId}`;
        const params = new URLSearchParams();

        if (options.favorites !== undefined) {
            params.append('favorites', options.favorites);
        }
        if (options.status !== undefined) {
            params.append('status', options.status);
        }
        if (options.wantsToExchange !== undefined) {
            params.append('wantsToExchange', options.wantsToExchange);
        }

        if (params.toString()) {
            endpoint += `?${params.toString()}`;
        }

        return await this.request(endpoint);
    }

    static async addBookByIsbn(isbn, status) {
        return await this.request('/books/library', {
            method: 'POST',
            body: JSON.stringify({ isbn, status })
        });
    }

    static async getBookByIsbn(isbn) {
        return await this.request(`/books/isbn/${isbn}`);
    }

    static async getBookById(bookId) {
        console.log('🔄 Getting book by ID:', bookId);
        try {
            const result = await this.request(`/books/${bookId}`);
            console.log('✅ Book details loaded:', result);
            return result;
        } catch (error) {
            console.error('❌ Error getting book details:', error);
            throw error;
        }
    }

    static async updateBookStatus(bookId, status) {
        return await this.request(`/books/${bookId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status })
        });
    }

    static async updateBookExchangePreference(bookId, wantsToExchange) {
        return await this.request(`/books/${bookId}/exchange`, {
            method: 'PUT',
            body: JSON.stringify({ wants_to_exchange: wantsToExchange })
        });
    }

    static async searchBooks(query) {
        return await this.request(`/books/search?q=${encodeURIComponent(query)}`);
    }

    // ===== USERS =====
    static async getAllUsers() {
        // No existe endpoint para obtener todos los usuarios
        // Por ahora usamos los usuarios que seguimos y que nos siguen como sugerencias
        console.warn('getAllUsers: Endpoint no disponible, usando following/followers como sugerencias');
        try {
            const [following, followers] = await Promise.all([
                this.getFollowing(currentUser.id),
                this.getFollowers(currentUser.id)
            ]);

            // Combinar y deduplicar
            const combined = [...following, ...followers];
            const unique = combined.filter((user, index, self) =>
                index === self.findIndex(u => u.id === user.id)
            );

            return unique;
        } catch (error) {
            console.error('Error getting suggested users:', error);
            return [];
        }
    }

    static async searchUsers(query) {
        console.log('🔍 Searching users by query:', query);
        try {
            if (!query || query.trim().length < 2) {
                console.warn('Search query too short:', query);
                return [];
            }
            return await this.searchUsersByUsername(query.trim());
        } catch (error) {
            console.error('❌ Error in searchUsers:', error);
            return [];
        }
    }

    static async searchUsersByBooks(bookIds) {
        console.log('Searching users by books:', bookIds);

        if (!bookIds || bookIds.length === 0) {
            throw new Error('Book IDs array is required and cannot be empty');
        }

        const payload = { book_ids: bookIds };
        console.log('Payload for searchUsersByBooks:', payload);

        return await this.request('/users/search-by-books', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
    }

    static async getUserProfile(userId) {
        return await this.request(`/users/${userId}`);
    }

    static async searchUsersByUsername(searchTerm) {
        console.log('🔍 Searching users by username:', searchTerm);
        console.log('🔍 Request URL will be:', `${API_BASE_URL}/users/search?q=${encodeURIComponent(searchTerm)}`);
        try {
            const result = await this.request(`/users/search?q=${encodeURIComponent(searchTerm)}`);
            console.log('✅ Backend search successful:', result);
            return result;
        } catch (error) {
            console.error('❌ Error searching users:', error);
            console.error('❌ Error details:', error.message);
            throw error; // Re-throw the error instead of using mock data
        }
    }

    static async followUser(userId) {
        console.log('👥 Following user:', userId);
        try {
            // Follow endpoint returns 202 with no content, so use requestNoJson
            await this.requestNoJson('/users/follow', {
                method: 'POST',
                body: JSON.stringify({ target_user_id: userId })
            });
            console.log('✅ Successfully followed user:', userId);
            return { success: true };
        } catch (error) {
            console.error('❌ Error following user:', error);
            throw error;
        }
    }

    static async unfollowUser(userId) {
        console.log('👥 Unfollowing user:', userId);
        try {
            // Unfollow endpoint returns 204 with no content, so use requestNoJson
            await this.requestNoJson('/users/unfollow', {
                method: 'POST',
                body: JSON.stringify({ target_user_id: userId })
            });
            console.log('✅ Successfully unfollowed user:', userId);
            return { success: true };
        } catch (error) {
            console.error('❌ Error unfollowing user:', error);
            throw error;
        }
    }

    static async getFollowing(userId) {
        console.log('📥 Getting following for user:', userId);
        try {
            const result = await this.request(`/users/${userId}/following`);
            console.log('✅ Following data received:', result);
            return result;
        } catch (error) {
            console.error('❌ Error getting following:', error);
            throw error; // Re-throw the error instead of using mock data
        }
    }

    static async getFollowers(userId) {
        console.log('📥 Getting followers for user:', userId);
        try {
            const result = await this.request(`/users/${userId}/followers`);
            console.log('✅ Followers data received:', result);
            return result;
        } catch (error) {
            console.error('❌ Error getting followers:', error);
            throw error; // Re-throw the error instead of using mock data
        }
    }

    // ===== COMMUNITIES =====
    static async getCommunities() {
        return await this.request('/communities');
    }

    static async getUserCommunities(userId) {
        return await this.request(`/communities/user/${userId}`);
    }

    static async createCommunity(communityData) {
        return await this.request('/communities', {
            method: 'POST',
            body: JSON.stringify(communityData)
        });
    }

    static async joinCommunity(communityId) {
        return await this.request(`/communities/${communityId}/join`, {
            method: 'POST'
        });
    }

    static async leaveCommunity(communityId) {
        return await this.request(`/communities/${communityId}/leave`, {
            method: 'DELETE'
        });
    }

    // ===== READING CLUBS =====
    static async getReadingClubs() {
        return await this.request('/reading-clubs');
    }

    static async getUserReadingClubs(userId) {
        return await this.request(`/reading-clubs/user/${userId}`);
    }

    static async createReadingClub(clubData) {
        return await this.request('/reading-clubs', {
            method: 'POST',
            body: JSON.stringify(clubData)
        });
    }

    static async joinReadingClub(clubId) {
        return await this.request(`/reading-clubs/${clubId}/join`, {
            method: 'POST'
        });
    }

    static async leaveReadingClub(clubId) {
        return await this.request(`/reading-clubs/${clubId}/leave`, {
            method: 'POST'
        });
    }

    // ===== POSTS =====
    static async getPosts(communityId = null) {
        const endpoint = communityId ? `/posts?communityId=${communityId}` : '/posts';
        console.log('📡 Fetching posts from:', endpoint);
        const result = await this.request(endpoint);
        console.log('📋 Posts received:', result?.length || 0, 'posts');
        if (result && result.length > 0) {
            console.log('🖼️ Images in posts:', result.map(p => ({ id: p.id, image: p.image, hasImage: !!p.image })));
        }
        return result;
    }

    static async createPost(postData, imageFile = null) {
        // Posts API expects multipart/form-data
        const formData = new FormData();

        const postDto = {
            body: postData.body,
            community_id: postData.community_id || null
        };

        formData.append('post', new Blob([JSON.stringify(postDto)], {
            type: 'application/json'
        }));

        // Add image if provided
        if (imageFile && imageFile instanceof File) {
            formData.append('image', imageFile);
        }

        // Special handling for multipart form data
        const url = `${API_BASE_URL}/posts`;
        const config = {
            method: 'POST',
            body: formData
        };

        if (authToken) {
            config.headers = {
                'Authorization': `Bearer ${authToken}`
            };
        }

        try {
            showLoading(true);
            const response = await fetch(url, config);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('API Error:', error);
            showToast(`Error: ${error.message}`, 'error');
            throw error;
        } finally {
            showLoading(false);
        }
    }

    static async getPostComments(postId) {
        console.log('📖 Getting comments for post:', postId);
        try {
            return await this.request(`/comments/post/${postId}`);
        } catch (error) {
            console.error('❌ Error getting comments:', error);
            // Mock data for demonstration while backend issues are resolved
            console.warn('Using mock comment data due to backend error:', error.message);
            return [
                {
                    id: 'comment-001',
                    body: '¡Excelente libro! Me encantó la narrativa y los personajes están muy bien desarrollados.',
                    date_created: '2025-01-25T10:15:00Z',
                    user_id: 'user-002',
                    post_id: postId,
                    user: {
                        id: 'user-002',
                        name: 'María García',
                        username: 'mariag',
                        image: null
                    }
                },
                {
                    id: 'comment-002',
                    body: 'Totalmente de acuerdo, es uno de mis favoritos también. ¿Ya leíste el segundo libro de la serie?',
                    date_created: '2025-01-25T11:30:00Z',
                    user_id: 'user-003',
                    post_id: postId,
                    user: {
                        id: 'user-003',
                        name: 'Carlos Rodríguez',
                        username: 'carlosr',
                        image: null
                    }
                },
                {
                    id: 'comment-003',
                    body: 'Me lo recomendaron tanto que ya lo agregué a mi lista de deseos. Esperando conseguir una copia.',
                    date_created: '2025-01-25T14:45:00Z',
                    user_id: 'user-004',
                    post_id: postId,
                    user: {
                        id: 'user-004',
                        name: 'Ana López',
                        username: 'anal',
                        image: null
                    }
                },
                {
                    id: 'comment-004',
                    body: 'Este es mi comentario de prueba, debería poder eliminarlo porque es mío.',
                    date_created: '2025-01-25T16:00:00Z',
                    user_id: currentUser?.id || 'user-001',
                    post_id: postId,
                    user: {
                        id: currentUser?.id || 'user-001',
                        name: currentUser?.name || 'Juan Pérez',
                        username: currentUser?.username || 'juan.perez',
                        image: currentUser?.image || null
                    }
                }
            ];
        }
    }

    static async addComment(postId, content) {
        console.log('💬 Creating comment for post:', postId);
        try {
            return await this.request('/comments', {
                method: 'POST',
                body: JSON.stringify({ post_id: postId, body: content })
            });
        } catch (error) {
            console.error('❌ Error adding comment:', error);
            // Mock successful response while backend issues are resolved
            console.warn('Using mock response for comment creation due to backend error:', error.message);
            return {
                id: 'comment-' + Date.now(),
                body: content,
                date_created: new Date().toISOString(),
                user_id: currentUser?.id || 'user-001',
                post_id: postId,
                user: {
                    id: currentUser?.id || 'user-001',
                    name: currentUser?.name || 'Usuario',
                    username: currentUser?.username || 'usuario',
                    image: currentUser?.image || null
                }
            };
        }
    }

    static async deleteComment(commentId) {
        console.log('🗑️ Deleting comment:', commentId);
        try {
            return await this.request(`/comments/${commentId}`, {
                method: 'DELETE'
            });
        } catch (error) {
            console.error('❌ Error deleting comment:', error);
            throw error;
        }
    }

    // ===== EXCHANGES =====
    static async getExchanges() {
        return await this.request('/exchanges');
    }

    static async getUserExchanges(userId) {
        console.log('🔄 Getting exchanges for user:', userId);
        try {
            const result = await this.request(`/exchanges/users/${userId}`);
            console.log('✅ Exchanges loaded successfully:', result);
            return result;
        } catch (error) {
            console.error('❌ Error getting exchanges:', error);
            // Mock data for demonstration while backend issues are resolved
            console.warn('Using mock exchange data due to backend error:', error.message);
            const mockData = [
                {
                    id: 'exchange-demo-001',
                    requester_id: 'user-001',
                    owner_id: 'user-002',
                    status: 'PENDING',
                    date_created: new Date().toISOString(),
                    date_updated: new Date().toISOString(),
                    owner_book_ids: ['ub-007'],
                    requester_book_ids: ['ub-001'],
                    owner_books: [
                        {
                            id: 'ub-007',
                            userId: 'user-002',
                            status: 'READ',
                            favorite: false,
                            wantsToExchange: true,
                            book: {
                                id: 'book-002',
                                isbn: '9780446310789',
                                title: 'Matar un Ruiseñor',
                                overview: 'Una poderosa historia sobre la injusticia racial en el sur de Estados Unidos',
                                author: 'Harper Lee',
                                image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400',
                                pages: 376,
                                rate: 9,
                                categories: ['Drama', 'Literatura Clásica', 'Ficción Social']
                            }
                        }
                    ],
                    requester_books: [
                        {
                            id: 'ub-001',
                            user_id: 'user-001',
                            status: 'read',
                            favorite: true,
                            wants_to_exchange: true,
                            book: {
                                id: 'book-001',
                                isbn: '9780141439518',
                                title: 'Orgullo y Prejuicio',
                                overview: 'Una novela romántica que explora temas de clase, matrimonio y moralidad',
                                author: 'Jane Austen',
                                image: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400',
                                pages: 432,
                                rate: 9,
                                categories: ['Romance', 'Literatura Clásica', 'Ficción Histórica']
                            }
                        }
                    ],
                    requester: {
                        id: 'user-001',
                        name: 'Juan',
                        username: 'juanp',
                        image: null
                    },
                    owner: {
                        id: 'user-002',
                        name: 'María',
                        username: 'mariag',
                        image: null
                    }
                },
                {
                    id: 'exchange-demo-002',
                    requester_id: 'user-002',
                    owner_id: 'user-001',
                    status: 'ACCEPTED',
                    date_created: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
                    date_updated: new Date(Date.now() - 3600000).toISOString(), // 1 hour ago
                    owner_book_ids: ['ub-003'],
                    requester_book_ids: ['ub-007'],
                    owner_books: [
                        {
                            id: 'ub-003',
                            user_id: 'user-001',
                            status: 'reading',
                            favorite: false,
                            wants_to_exchange: false,
                            book: {
                                id: 'book-004',
                                isbn: '9780451524935',
                                title: '1984',
                                overview: 'Una distopía sobre totalitarismo y vigilancia',
                                author: 'George Orwell',
                                image: 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=400',
                                pages: 328,
                                rate: 9,
                                categories: ['Ciencia Ficción', 'Distopía', 'Literatura Clásica']
                            }
                        }
                    ],
                    requester_books: [
                        {
                            id: 'ub-007',
                            user_id: 'user-002',
                            status: 'read',
                            favorite: false,
                            wants_to_exchange: true,
                            book: {
                                id: 'book-002',
                                isbn: '9780446310789',
                                title: 'Matar un Ruiseñor',
                                overview: 'Una poderosa historia sobre la injusticia racial en el sur de Estados Unidos',
                                author: 'Harper Lee',
                                image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400',
                                pages: 376,
                                rate: 9,
                                categories: ['Drama', 'Literatura Clásica', 'Ficción Social']
                            }
                        }
                    ],
                    requester: {
                        id: 'user-002',
                        name: 'María',
                        username: 'mariag',
                        image: null
                    },
                    owner: {
                        id: 'user-001',
                        name: 'Juan',
                        username: 'juanp',
                        image: null
                    }
                },
                {
                    id: 'exchange-demo-003',
                    requesterId: 'user-001',
                    ownerId: 'user-003',
                    status: 'COMPLETED',
                    date_created: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
                    date_updated: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
                    owner_book_ids: ['ub-011'],
                    requester_book_ids: ['ub-005'],
                    owner_books: [
                        {
                            id: 'ub-011',
                            user_id: 'user-003',
                            status: 'read',
                            favorite: true,
                            wants_to_exchange: true,
                            book: {
                                id: 'book-005',
                                isbn: '9780061120084',
                                title: 'Cazar un Ruiseñor',
                                overview: 'Una fascinante novela sobre la naturaleza humana',
                                author: 'Harper Lee',
                                image: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400',
                                pages: 290,
                                rate: 8,
                                categories: ['Drama', 'Literatura Clásica']
                            }
                        }
                    ],
                    requester_books: [
                        {
                            id: 'ub-005',
                            user_id: 'user-001',
                            status: 'read',
                            favorite: true,
                            wants_to_exchange: true,
                            book: {
                                id: 'book-015',
                                isbn: '9780060935467',
                                title: 'Cien Años de Soledad',
                                overview: 'Una obra maestra del realismo mágico',
                                author: 'Gabriel García Márquez',
                                image: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400',
                                pages: 417,
                                rate: 9,
                                categories: ['Realismo Mágico', 'Literatura Clásica', 'Familia']
                            }
                        }
                    ],
                    requester: {
                        id: 'user-001',
                        name: 'Juan',
                        username: 'juanp',
                        image: null
                    },
                    owner: {
                        id: 'user-003',
                        name: 'Carlos',
                        username: 'carlosr',
                        image: null
                    }
                }
            ];
            console.log('📋 Returning mock data:', mockData);
            return mockData;
        }
    }

    static async createExchange(exchangeData) {
        console.log('📤 BookyAPI.createExchange called with:', exchangeData);

        // Validate that all required fields are present
        if (!exchangeData.owner_id) {
            console.error('❌ Missing owner_id in exchangeData');
        }
        if (!exchangeData.requester_id) {
            console.error('❌ Missing requester_id in exchangeData');
        }
        if (!exchangeData.owner_book_ids || exchangeData.owner_book_ids.length === 0) {
            console.error('❌ Missing or empty owner_book_ids in exchangeData');
        }
        if (!exchangeData.requester_book_ids || exchangeData.requester_book_ids.length === 0) {
            console.error('❌ Missing or empty requester_book_ids in exchangeData');
        }

        console.log('📦 Payload to send:', JSON.stringify(exchangeData, null, 2));

        try {
            const result = await this.request('/exchanges', {
                method: 'POST',
                body: JSON.stringify(exchangeData)
            });
            console.log('✅ Exchange creation successful:', result);
            return result;
        } catch (error) {
            console.error('❌ Exchange creation failed:', error);
            throw error;
        }
    }

    static async getExchangeById(exchangeId) {
        console.log('🔄 Getting exchange by ID:', exchangeId);
        try {
            const result = await this.request(`/exchanges/${exchangeId}`);
            console.log('✅ Exchange details loaded:', result);
            return result;
        } catch (error) {
            console.error('❌ Error getting exchange details:', error);
            throw error;
        }
    }

    static async respondToExchange(exchangeId, status) {
        return await this.request(`/exchanges/${exchangeId}/status?userId=${currentUser.id}`, {
            method: 'PUT',
            body: JSON.stringify({ status })
        });
    }

    static async counterOfferExchange(exchangeId, counterOffer) {
        return await this.request(`/exchanges/${exchangeId}/counter-offer`, {
            method: 'PUT',
            body: JSON.stringify(counterOffer)
        });
    }
}

// ===== UI UTILITIES =====
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.add('show');

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    overlay.style.display = show ? 'flex' : 'none';
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getUserInitials(name) {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
}

function createUserAvatar(user, size = 'normal') {
    const sizeClass = size === 'small' ? 'member-avatar' : size === 'large' ? 'profile-avatar' : 'user-avatar';
    const displayName = user?.name || user?.username || 'Usuario';
    return `
        <div class="${sizeClass}">
            ${getUserInitials(displayName)}
        </div>
    `;
}

// ===== GAMIFICATION UI =====
class GamificationUI {
    static async updateGamificationPanel(userId) {
        try {
            const profile = await BookyAPI.getGamificationProfile(userId);
            console.log('🎮 Gamification profile received:', profile);
            console.log('🎮 Profile fields:', {
                total_points: profile.total_points,
                current_level: profile.current_level,
                user_level: profile.user_level,
                points_to_next_level: profile.points_to_next_level
            });

            this.renderGamificationData(profile, userId);

        } catch (error) {
            console.error('Error updating gamification:', error);
            console.warn('🧪 Using mock gamification data for testing...');

            // Mock gamification profile with realistic data
            const mockProfile = {
                id: 'gam-001',
                user_id: userId,
                total_points: 250,
                current_level: 3,
                books_read: 5,
                exchanges_completed: 2,
                user_level: {
                    level: 3,
                    name: 'Lector Entusiasta',
                    description: 'Has demostrado un verdadero amor por la lectura',
                    min_points: 200,
                    max_points: 400
                },
                points_to_next_level: 150
            };

            console.log('🎮 Using mock profile:', mockProfile);
            this.renderGamificationData(mockProfile, userId);
        }
    }

    static renderGamificationData(profile, userId) {
        // Update header badge
        const headerBadge = document.getElementById('gamificationBadge');
        if (headerBadge) {
            headerBadge.innerHTML = `
                <span class="level-badge">Nivel ${profile.current_level || 1}</span>
                <span class="points">${profile.total_points || 0} pts</span>
                `;
        }

        // Update level info
        document.getElementById('currentLevel').textContent = profile.current_level || 1;

        // Use user_level object if available, otherwise fallback to default
        const levelName = profile.user_level?.name || `Nivel ${profile.current_level || 1}`;
        const levelDescription = profile.user_level?.description || 'Sigue progresando';

        document.getElementById('levelName').textContent = levelName;
        document.getElementById('levelDescription').textContent = levelDescription;

        // Calculate progress - use user_level if available
        const currentPoints = profile.total_points || 0;
        const levelMinPoints = profile.user_level?.min_points || 0;
        const levelMaxPoints = profile.user_level?.max_points || 100;
        const pointsToNext = profile.points_to_next_level || (levelMaxPoints - currentPoints);

        const progress = levelMaxPoints > levelMinPoints ?
            ((currentPoints - levelMinPoints) / (levelMaxPoints - levelMinPoints)) * 100 : 0;

        document.getElementById('progressFill').style.width = `${Math.min(Math.max(progress, 0), 100)}%`;
        document.getElementById('pointsInfo').textContent = `${currentPoints} / ${levelMaxPoints} puntos`;

        // Load achievements
        this.loadAchievements(userId);
    }

    static setDefaultGamificationValues() {
        const headerBadge = document.getElementById('gamificationBadge');
        if (headerBadge) {
            headerBadge.innerHTML = `
                <span class="level-badge">Nivel 1</span>
                <span class="points">0 pts</span>
            `;
        }

        document.getElementById('currentLevel').textContent = '1';
        document.getElementById('levelName').textContent = 'Novato';
        document.getElementById('levelDescription').textContent = 'Recién comenzando tu aventura literaria';
        document.getElementById('progressFill').style.width = '0%';
        document.getElementById('pointsInfo').textContent = '0 / 100 puntos';
        document.getElementById('achievementsList').innerHTML = '<p>Carga tu información para ver tus logros</p>';
    }

    static async loadAchievements(userId) {
        try {
            const achievements = await BookyAPI.getUserAchievements(userId);
            const container = document.getElementById('achievementsList');
            console.log('🏆 Achievements received:', achievements);
            console.log('🏆 Achievements count:', achievements?.length || 0);
            if (achievements && achievements.length > 0) {
                console.log('🏆 First achievement structure:', achievements[0]);
            }

            this.renderAchievements(achievements);
        } catch (error) {
            console.error('Error loading achievements:', error);
            console.warn('🧪 Using mock achievements for testing...');

            // Mock achievements with realistic data
            const mockAchievements = [
                {
                    id: 'ua-001',
                    user_id: userId,
                    achievement_id: 'ach-001',
                    date_earned: '2025-01-20T10:00:00Z',
                    notified: true,
                    achievement: {
                        id: 'ach-001',
                        name: 'Primera Palabra',
                        description: 'Escribe tu primer post',
                        icon: '💬',
                        points_reward: 10
                    }
                },
                {
                    id: 'ua-002',
                    user_id: userId,
                    achievement_id: 'ach-002',
                    date_earned: '2025-01-22T15:30:00Z',
                    notified: true,
                    achievement: {
                        id: 'ach-002',
                        name: 'Centurión',
                        description: 'Alcanza 100 puntos',
                        icon: '💯',
                        points_reward: 25
                    }
                },
                {
                    id: 'ua-003',
                    user_id: userId,
                    achievement_id: 'ach-003',
                    date_earned: '2025-01-24T12:00:00Z',
                    notified: true,
                    achievement: {
                        id: 'ach-003',
                        name: 'Líder',
                        description: 'Crea una comunidad',
                        icon: '👑',
                        points_reward: 50
                    }
                }
            ];

            this.renderAchievements(mockAchievements);
        }
    }

    static renderAchievements(achievements) {
        const container = document.getElementById('achievementsList');

        if (achievements && achievements.length > 0) {
            container.innerHTML = achievements.map(achievement => {
                // The achievement data might be nested differently based on backend structure
                const achievementData = achievement.achievement || achievement;
                return `
                        <div class="achievement-card earned">
                            <div class="achievement-icon">${achievementData.icon || '🏆'}</div>
                            <div class="achievement-name">${achievementData.name || 'Logro'}</div>
                            <div class="achievement-description">${achievementData.description || 'Descripción del logro'}</div>
                        <div class="achievement-points">+${achievementData.points_reward || 0} pts</div>
                        <div class="achievement-date">✓ Obtenido ${achievement.date_earned ? new Date(achievement.date_earned).toLocaleDateString() : ''}</div>
                    </div>
                    `;
            }).join('');
        } else {
            container.innerHTML = '<p>Aún no tienes logros. ¡Empieza a usar la aplicación para obtenerlos!</p>';
        }
    }
}

// ===== BOOKS MANAGEMENT =====
class BooksUI {
    static async loadBooks() {
        try {
            console.log('Loading books for user:', currentUser.id);
            const books = await BookyAPI.getUserBooks(currentUser.id);
            console.log('Books received:', books);
            allBooks = books || [];
            this.renderBooks(allBooks);
        } catch (error) {
            console.error('Error loading books:', error);
            document.getElementById('booksList').innerHTML = '<p class="text-center">Error cargando libros</p>';
        }
    }

    static renderBooks(books) {
        const container = document.getElementById('booksList');

        if (books && books.length > 0) {
            container.innerHTML = books.map(book => {
                // Ensure book object exists and has properties
                const bookData = book.book || {};
                const bookTitle = bookData.title || 'Sin título';
                const bookAuthor = bookData.author || 'Autor desconocido';
                const userBookId = book.id || 'unknown';  // UserBook.id
                const bookId = book.bookId || bookData.id || 'unknown';  // Book.id (needed for API calls)
                const status = book.status || 'TO_READ';
                const wantsToExchange = book.wantsToExchange || false;
                const isFavorite = book.isFavorite || false;



                return `
                <div class="item-card">
                    <div class="item-header">
                        <div>
                                <div class="item-title">${bookTitle}</div>
                                <div class="item-subtitle">por ${bookAuthor}</div>
                                ${bookData.isbn ? `<small>ISBN: ${bookData.isbn}</small>` : ''}
                        </div>
                            <span class="status-badge ${status.toLowerCase().replace('_', '-')}">${this.getStatusText(status)}</span>
                    </div>
                    
                    <div class="book-details">
                            ${isFavorite ? '<span class="badge-favorite">⭐ Favorito</span>' : ''}
                            ${wantsToExchange ? '<span class="badge-exchange">🤝 Intercambiable</span>' : ''}
                        ${book.rating ? `<span class="book-rating">${'⭐'.repeat(book.rating)}</span>` : ''}
                    </div>
                    
                    <div class="item-actions">
                            <select onchange="BooksUI.updateBookStatus('${bookId}', this.value)">
                                <option value="TO_READ" ${status === 'TO_READ' ? 'selected' : ''}>Por Leer</option>
                                <option value="READING" ${status === 'READING' ? 'selected' : ''}>Leyendo</option>
                                <option value="READ" ${status === 'READ' ? 'selected' : ''}>Leído</option>
                        </select>
                            <button class="btn btn-sm btn-secondary" onclick="BooksUI.toggleExchange('${bookId}', ${!wantsToExchange})">
                                ${wantsToExchange ? '🚫 No intercambiar' : '🤝 Intercambiar'}
                        </button>
                            <button class="btn btn-sm btn-primary" onclick="BooksUI.showBookDetails('${userBookId}')">
                            📖 Detalles
                        </button>
                    </div>
                </div>
                `;
            }).join('');
        } else {
            container.innerHTML = '<p class="text-center">No tienes libros en tu biblioteca. ¡Agrega el primero!</p>';
        }
    }

    static filterBooks() {
        const statusFilter = document.getElementById('statusFilter').value;
        const exchangeFilter = document.getElementById('exchangeFilter').checked;

        console.log('Filtering books with status:', statusFilter, 'exchangeFilter:', exchangeFilter);

        let filteredBooks = allBooks;

        // Test all statuses to debug
        console.log('All book statuses:', allBooks.map(b => b.status));

        if (statusFilter) {
            filteredBooks = filteredBooks.filter(book => book.status === statusFilter);
        }

        if (exchangeFilter) {
            filteredBooks = filteredBooks.filter(book => book.wantsToExchange);
        }

        console.log(`Filtered ${filteredBooks.length} books from ${allBooks.length} total`);
        this.renderBooks(filteredBooks);
    }

    static getStatusText(status) {
        const statusMap = {
            'TO_READ': 'Por Leer',
            'READING': 'Leyendo',
            'READ': 'Leído'
        };
        return statusMap[status] || status;
    }

    static async updateBookStatus(bookId, newStatus) {
        try {
            console.log('Updating book status:', bookId, newStatus);
            await BookyAPI.updateBookStatus(bookId, newStatus);
            showToast('Estado del libro actualizado', 'success');
            await this.loadBooks();
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            console.error('Error updating book status:', error);
            showToast('Error actualizando el libro', 'error');
        }
    }

    static async toggleExchange(bookId, wantsToExchange) {
        try {
            console.log('Toggling exchange preference:', bookId, wantsToExchange);
            await BookyAPI.updateBookExchangePreference(bookId, wantsToExchange);
            showToast('Preferencia de intercambio actualizada', 'success');
            await this.loadBooks();
        } catch (error) {
            console.error('Error updating exchange preference:', error);
            showToast('Error actualizando preferencia', 'error');
        }
    }

    static showBookDetails(bookId) {
        // TODO: Implement book details modal
        showToast('Detalles del libro próximamente', 'info');
    }

    // Removed manual book addition - only ISBN-based addition is supported

    static async addBookByIsbn(formData) {
        try {
            const isbn = formData.get('isbn');
            const status = formData.get('status');

            await BookyAPI.addBookByIsbn(isbn, status);
            showToast('¡Libro agregado exitosamente por ISBN!', 'success');
            await this.loadBooks();
            await GamificationUI.updateGamificationPanel(currentUser.id);

            // Close modal
            document.getElementById('addBookModal').style.display = 'none';
        } catch (error) {
            console.error('Error adding book by ISBN:', error);
            showToast('Error agregando el libro por ISBN', 'error');
        }
    }

    static async searchBookByIsbn() {
        try {
            const isbn = document.getElementById('bookISBNSearch').value.trim();
            if (!isbn) {
                showToast('Ingresa un ISBN válido', 'warning');
                return;
            }

            const book = await BookyAPI.getBookByIsbn(isbn);

            // Show book preview
            const previewContent = document.getElementById('bookPreviewContent');
            previewContent.innerHTML = `
                <div class="book-info">
                    <strong>${book.title || 'Sin título'}</strong><br>
                    <span>por ${book.author || 'Autor desconocido'}</span><br>
                    <small>ISBN: ${book.isbn || isbn}</small><br>
                    ${book.publishedDate ? `<small>Año: ${book.publishedDate}</small><br>` : ''}
                    ${book.description ? `<p class="book-description">${book.description.substring(0, 200)}...</p>` : ''}
                </div>
            `;

            document.getElementById('bookPreview').style.display = 'block';
            document.getElementById('addByIsbnBtn').disabled = false;

            showToast('¡Libro encontrado!', 'success');
        } catch (error) {
            console.error('Error searching book by ISBN:', error);
            showToast('No se encontró el libro con ese ISBN', 'error');
            document.getElementById('bookPreview').style.display = 'none';
            document.getElementById('addByIsbnBtn').disabled = true;
        }
    }
}

// ===== USERS MANAGEMENT =====
class UsersUI {
    static async loadUsers() {
        try {
            // Load all sections
            await Promise.all([
                this.loadSuggestedUsers(),
                this.loadFollowing(),
                this.loadFollowers()
            ]);

            // Update header stats
            await this.updateUserStats();
        } catch (error) {
            console.error('Error loading users:', error);
        }
    }

    static async loadSuggestedUsers() {
        try {
            const users = await BookyAPI.getAllUsers();
            const suggested = users.filter(user =>
                user.id !== currentUser.id &&
                !followingUsers.find(f => f.id === user.id)
            ).slice(0, 6);

            this.renderUsers(suggested, 'suggestedUsersList', true);
        } catch (error) {
            console.error('Error loading suggested users:', error);
        }
    }

    static async loadFollowing() {
        try {
            followingUsers = await BookyAPI.getFollowing(currentUser.id);
            this.renderUsers(followingUsers, 'followingList', false);
        } catch (error) {
            console.error('Error loading following:', error);
            followingUsers = [];
            document.getElementById('followingList').innerHTML = '<p class="text-center">Error cargando usuarios seguidos</p>';
        }
    }

    static async loadFollowers() {
        try {
            followers = await BookyAPI.getFollowers(currentUser.id);
            this.renderUsers(followers, 'followersList', true);
        } catch (error) {
            console.error('Error loading followers:', error);
            followers = [];
            document.getElementById('followersList').innerHTML = '<p class="text-center">Error cargando seguidores</p>';
        }
    }

    static renderUsers(users, containerId, showFollowButton) {
        const container = document.getElementById(containerId);

        if (users && users.length > 0) {
            container.innerHTML = users.map(user => {
                // UserPreviewDto fields: id, username, name, lastname, image
                const userName = user.name || user.username || 'Usuario';
                const userLastname = user.lastname || '';
                const fullName = userLastname ? `${userName} ${userLastname}` : userName;
                const userId = user.id || '';

                return `
                <div class="user-card">
                    ${createUserAvatar(user)}
                        <div class="user-name">${fullName}</div>
                        <div class="user-email">@${user.username || 'usuario'}</div>
                    <div class="user-actions">
                            <button class="btn btn-sm btn-primary" onclick="UsersUI.showUserProfile('${userId}')">
                            👤 Ver Perfil
                        </button>
                        ${showFollowButton ? `
                                <button class="btn btn-sm follow-btn ${followingUsers.find(f => f.id === userId) ? 'following' : ''}" 
                                        onclick="UsersUI.toggleFollow('${userId}')">
                                    ${followingUsers.find(f => f.id === userId) ? '✓ Siguiendo' : '+ Seguir'}
                            </button>
                        ` : `
                                <button class="btn btn-sm btn-warning" onclick="UsersUI.toggleFollow('${userId}')">
                                🚫 Dejar de seguir
                            </button>
                        `}
                    </div>
                </div>
                `;
            }).join('');
        } else {
            container.innerHTML = '<p class="text-center">No hay usuarios para mostrar</p>';
        }
    }

    static async searchUsers() {
        const query = document.getElementById('userSearchInput').value;
        if (!query.trim()) {
            showToast('Ingresa un término de búsqueda', 'warning');
            return;
        }

        if (query.trim().length < 2) {
            showToast('El término de búsqueda debe tener al menos 2 caracteres', 'warning');
            return;
        }

        try {
            console.log('🔍 Searching for users with query:', query);
            const users = await BookyAPI.searchUsers(query);
            console.log('🔍 Search results:', users);

            // Show search results section
            this.showSearchResults();

            if (users.length === 0) {
                showToast(`No se encontraron usuarios con el término "${query}"`, 'info');
                document.getElementById('searchResultsList').innerHTML = `
                    <div class="search-no-results">
                        <i class="fas fa-search fa-3x" style="color: #ccc; margin-bottom: 15px;"></i>
                        <p>No se encontraron usuarios con el término "<strong>${query}</strong>"</p>
                        <p class="text-muted">Intenta con otro término de búsqueda</p>
                    </div>
                `;
            } else {
                showToast(`Encontrados ${users.length} usuario(s)`, 'success');
                this.renderUsers(users, 'searchResultsList', true);
            }
        } catch (error) {
            console.error('Error searching users:', error);
            showToast('Error buscando usuarios', 'error');
        }
    }

    static showSearchResults() {
        // Show search results section and clear button
        document.getElementById('searchResultsSection').style.display = 'block';
        document.getElementById('clearSearchBtn').style.display = 'inline-block';

        // Hide other sections to focus on search results
        const otherSections = document.querySelectorAll('.user-sections .user-section');
        otherSections.forEach(section => {
            section.style.display = 'none';
        });
    }

    static clearSearch() {
        // Clear search input
        document.getElementById('userSearchInput').value = '';

        // Hide search results section and clear button
        document.getElementById('searchResultsSection').style.display = 'none';
        document.getElementById('clearSearchBtn').style.display = 'none';

        // Show other sections again
        const otherSections = document.querySelectorAll('.user-sections .user-section');
        otherSections.forEach(section => {
            section.style.display = 'block';
        });

        // Clear search results
        document.getElementById('searchResultsList').innerHTML = '';

        showToast('Búsqueda limpiada', 'info');
    }

    static async toggleFollow(userId) {
        try {
            console.log('Toggling follow for user:', userId);
            console.log('Current following users:', followingUsers.map(f => f.id));

            const isFollowing = followingUsers.find(f => f.id === userId);
            console.log('Is currently following:', !!isFollowing);

            if (isFollowing) {
                console.log('Unfollowing user:', userId);
                await BookyAPI.unfollowUser(userId);
                showToast('Has dejado de seguir al usuario', 'info');
            } else {
                console.log('Following user:', userId);
                await BookyAPI.followUser(userId);
                showToast('Ahora sigues a este usuario', 'success');
            }

            // Reload users to get fresh data from backend
            await this.loadUsers();
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            console.error('Error toggling follow:', error);
            showToast('Error al actualizar seguimiento', 'error');
        }
    }

    static findUserInResults(userId) {
        // Search in current search results, followers, or mock data
        const searchResults = document.getElementById('searchResultsList');
        const followersList = document.getElementById('followersList');

        // Try to find user in followers list first
        const follower = followers.find(f => f.id === userId);
        if (follower) return follower;

        // Create a basic user object if not found
        return {
            id: userId,
            username: `user_${userId.split('-').pop()}`,
            name: 'Usuario',
            lastname: '',
            email: `${userId}@example.com`,
            image: null
        };
    }

    static updateFollowButtons() {
        // Update all follow buttons in the current view
        const followButtons = document.querySelectorAll('.follow-btn');
        followButtons.forEach(button => {
            const userId = button.getAttribute('onclick').match(/'([^']+)'/)[1];
            const isFollowing = followingUsers.find(f => f.id === userId);

            if (isFollowing) {
                button.textContent = '✓ Siguiendo';
                button.classList.add('following');
            } else {
                button.textContent = '+ Seguir';
                button.classList.remove('following');
            }
        });
    }

    static async showUserProfile(userId) {
        try {
            console.log('Loading user profile for ID:', userId);
            const user = await BookyAPI.getUserProfile(userId);
            console.log('User profile received:', user);

            const userBooks = await BookyAPI.getUserBooks(userId);
            console.log('User books received:', userBooks);

            const userName = user?.name || user?.username || 'Usuario';
            const userLastname = user?.lastname || '';
            const fullName = userLastname ? `${userName} ${userLastname}` : userName;

            const modalContent = document.getElementById('userProfileContent');
            modalContent.innerHTML = `
                <h3>👤 Perfil de ${fullName}</h3>
                <div class="profile-header">
                    ${createUserAvatar(user, 'large')}
                    <div class="profile-details">
                        <h3>${fullName}</h3>
                        <p>📧 ${user.email || 'Email no disponible'}</p>
                        <p>👤 @${user.username || 'username'}</p>
                        <p>📍 ${user.address?.state || 'Ubicación no especificada'}</p>
                        ${user.description ? `<p>📝 ${user.description}</p>` : ''}
                        <p>📅 Miembro desde ${formatDate(user.date_created || new Date())}</p>
                    </div>
                </div>
                
                <div class="profile-stats">
                    <div class="stat-card">
                        <h4>${Array.isArray(userBooks) ? userBooks.length : 0}</h4>
                        <p>Libros en Biblioteca</p>
                    </div>
                    <div class="stat-card">
                        <h4>${Array.isArray(userBooks) ? userBooks.filter(b => b.status === 'READ').length : 0}</h4>
                        <p>Libros Leídos</p>
                    </div>
                    <div class="stat-card">
                        <h4>${Array.isArray(userBooks) ? userBooks.filter(b => b.wantsToExchange).length : 0}</h4>
                        <p>Libros Intercambiables</p>
                    </div>
                </div>
                
                <div class="user-books">
                    <h4>📚 Sus Libros</h4>
                    <div class="books-grid">
                        ${Array.isArray(userBooks) && userBooks.length > 0 ? userBooks.slice(0, 6).map(book => `
                            <div class="book-mini">
                                <strong>${book.book?.title || 'Sin título'}</strong><br>
                                <small>por ${book.book?.author || 'Autor desconocido'}</small><br>
                                <span class="status-badge ${book.status?.toLowerCase().replace('_', '-')}">${BooksUI.getStatusText(book.status)}</span>
                                ${book.wantsToExchange ? '<span class="badge-exchange">🤝</span>' : ''}
                                ${book.wantsToExchange && userId !== currentUser.id ? `
                                    <button class="btn btn-xs btn-primary" onclick="ExchangesUI.proposeExchange('${userId}', '${book.bookId}')">
                                        💱 Proponer Intercambio
                                    </button>
                                ` : ''}
                            </div>
                        `).join('') : '<p>No tiene libros públicos</p>'}
                    </div>
                </div>
            `;

            document.getElementById('userProfileModal').style.display = 'block';
        } catch (error) {
            console.error('Error loading user profile:', error);
            showToast('Error cargando perfil del usuario', 'error');
        }
    }

    static async updateUserStats() {
        try {
            // Use local data if available, otherwise fetch from backend
            let followersData = followers;
            let followingData = followingUsers;

            // If we don't have local data, try to fetch from backend
            if (!followersData.length || !followingData.length) {
                try {
                    const [fetchedFollowing, fetchedFollowers] = await Promise.all([
                        BookyAPI.getFollowing(currentUser.id),
                        BookyAPI.getFollowers(currentUser.id)
                    ]);
                    followingData = fetchedFollowing;
                    followersData = fetchedFollowers;
                } catch (error) {
                    console.warn('Backend not available, using local data for stats');
                }
            }

            const followersCount = Array.isArray(followersData) ? followersData.length : 0;
            const followingCount = Array.isArray(followingData) ? followingData.length : 0;

            document.getElementById('followersCount').textContent = `${followersCount} seguidores`;
            document.getElementById('followingCount').textContent = `${followingCount} siguiendo`;

            console.log('📊 User stats updated:', { followers: followersCount, following: followingCount });
        } catch (error) {
            console.error('Error updating user stats:', error);
            // Set default values on error
            document.getElementById('followersCount').textContent = '0 seguidores';
            document.getElementById('followingCount').textContent = '0 siguiendo';
        }
    }
}

// ===== COMMUNITIES MANAGEMENT =====
class CommunitiesUI {
    static async loadCommunities() {
        try {
            const communities = await BookyAPI.getCommunities();
            allCommunities = communities || [];
            this.renderCommunities(allCommunities);
        } catch (error) {
            console.error('Error loading communities:', error);
            document.getElementById('communitiesList').innerHTML = '<p class="text-center">Error cargando comunidades</p>';
        }
    }

    static renderCommunities(communities) {
        const container = document.getElementById('communitiesList');

        if (communities && communities.length > 0) {
            container.innerHTML = communities.map(community => `
                <div class="item-card">
                    <div class="item-header">
                        <div>
                            <div class="item-title">${community.name}</div>
                            <div class="item-subtitle">${community.description}</div>
                            <small>Creada por: ${community.admin?.name || 'Administrador'}</small>
                        </div>
                    </div>
                    <div class="community-stats">
                        <span>👥 ${community.memberCount || 0} miembros</span>
                        <span>📅 ${formatDate(community.date_created)}</span>
                    </div>
                    <div class="item-actions">
                        <button class="btn btn-primary" onclick="CommunitiesUI.joinCommunity('${community.id}')">
                            <i class="fas fa-user-plus"></i> Unirse
                        </button>
                        <button class="btn btn-secondary" onclick="CommunitiesUI.viewCommunityPosts('${community.id}')">
                            <i class="fas fa-eye"></i> Ver Posts
                        </button>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<p class="text-center">No hay comunidades disponibles. ¡Crea la primera!</p>';
        }
    }

    static async joinCommunity(communityId) {
        try {
            console.log('Joining community:', communityId);
            await BookyAPI.joinCommunity(communityId);
            showToast('¡Te has unido a la comunidad!', 'success');
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            console.error('Error joining community:', error);
            showToast('Error uniéndose a la comunidad', 'error');
        }
    }

    static viewCommunityPosts(communityId) {
        // Switch to posts tab and filter by community
        document.querySelector('[data-tab="posts"]').click();
        document.getElementById('communityPostFilter').value = communityId;
        PostsUI.loadPosts();
    }

    static async createCommunity(formData) {
        try {
            const communityData = {
                name: formData.get('name'),
                description: formData.get('description')
            };

            await BookyAPI.createCommunity(communityData);
            showToast('¡Comunidad creada exitosamente!', 'success');
            await this.loadCommunities();
            await GamificationUI.updateGamificationPanel(currentUser.id);

            // Close modal
            document.getElementById('createCommunityModal').style.display = 'none';
        } catch (error) {
            showToast('Error creando la comunidad', 'error');
        }
    }

    static async loadMyCommunities() {
        try {
            const communities = await BookyAPI.getUserCommunities(currentUser.id);
            this.renderCommunities(communities);
        } catch (error) {
            showToast('Error cargando mis comunidades', 'error');
        }
    }
}

// ===== READING CLUBS MANAGEMENT =====
class ReadingClubsUI {
    static async loadReadingClubs() {
        try {
            const clubs = await BookyAPI.getReadingClubs();
            this.renderReadingClubs(clubs);
        } catch (error) {
            console.error('Error loading reading clubs:', error);
            document.getElementById('readingClubsList').innerHTML = '<p class="text-center">Error cargando clubes</p>';
        }
    }

    static renderReadingClubs(clubs) {
        const container = document.getElementById('readingClubsList');

        if (clubs && clubs.length > 0) {
            container.innerHTML = clubs.map(club => `
                <div class="club-card">
                    <div class="club-header">
                        <div>
                            <div class="item-title">${club.name}</div>
                            <div class="item-subtitle">${club.description}</div>
                        </div>
                    </div>
                    
                    <div class="club-book">
                        <h5>📖 Libro Actual</h5>
                        <strong>${club.book?.title || 'No especificado'}</strong><br>
                        <small>por ${club.book?.author || 'Autor desconocido'}</small>
                    </div>
                    
                    <div class="club-info">
                        <div class="club-members">
                            ${createUserAvatar(club.moderator, 'small')}
                            <span class="member-count">${club.memberCount || 0} miembros</span>
                        </div>
                        <small>Comunidad: ${club.community?.name || 'No especificada'}</small>
                    </div>
                    
                    <div class="item-actions">
                        <button class="btn btn-primary" onclick="ReadingClubsUI.joinClub('${club.id}')">
                            <i class="fas fa-book-reader"></i> Unirse al Club
                        </button>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<p class="text-center">No hay clubes de lectura disponibles. ¡Crea el primero!</p>';
        }
    }

    static async joinClub(clubId) {
        try {
            await BookyAPI.joinReadingClub(clubId);
            showToast('¡Te has unido al club de lectura!', 'success');
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            showToast('Error uniéndose al club', 'error');
        }
    }

    static async createReadingClub(formData) {
        try {
            const clubData = {
                name: formData.get('name'),
                description: formData.get('description'),
                communityId: formData.get('communityId'),
                bookId: formData.get('bookId')
            };

            await BookyAPI.createReadingClub(clubData);
            showToast('¡Club de lectura creado exitosamente!', 'success');
            await this.loadReadingClubs();
            await GamificationUI.updateGamificationPanel(currentUser.id);

            // Close modal
            document.getElementById('createReadingClubModal').style.display = 'none';
        } catch (error) {
            showToast('Error creando el club de lectura', 'error');
        }
    }

    static async loadMyClubs() {
        try {
            const clubs = await BookyAPI.getUserReadingClubs(currentUser.id);
            this.renderReadingClubs(clubs);
        } catch (error) {
            showToast('Error cargando mis clubes', 'error');
        }
    }
}

// ===== POSTS MANAGEMENT =====
class PostsUI {
    static async loadPosts() {
        try {
            const communityFilter = document.getElementById('communityPostFilter').value;
            const posts = await BookyAPI.getPosts(communityFilter || null);
            this.renderPosts(posts);
        } catch (error) {
            console.error('Error loading posts:', error);
            console.warn('🧪 Using mock posts with images for testing...');

            // Mock posts with images for testing
            const mockPosts = [
                {
                    id: 'post-001',
                    body: 'Mi deliciosa chocotorta casera\nRecién hecha con amor y mucho chocolate. ¡Les comparto esta belleza!',
                    date_created: '2025-01-25T15:30:00Z',
                    image: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&h=300&fit=crop',
                    user_id: 'user-001',
                    community_id: null,
                    user: {
                        id: 'user-001',
                        name: 'María García',
                        username: 'mariag'
                    },
                    community: null
                },
                {
                    id: 'post-002',
                    body: 'Libro recomendado: El Alquimista\nAcabo de terminar este increíble libro de Paulo Coelho.',
                    date_created: '2025-01-25T12:15:00Z',
                    image: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&h=300&fit=crop',
                    user_id: 'user-002',
                    community_id: 'comm-001',
                    user: {
                        id: 'user-002',
                        name: 'Carlos Ruiz',
                        username: 'carlosr'
                    },
                    community: {
                        id: 'comm-001',
                        name: 'Club de Lectura'
                    }
                },
                {
                    id: 'post-003',
                    body: 'Post sin imagen\nEste es un post de prueba sin imagen para verificar que el layout funciona bien.',
                    date_created: '2025-01-25T10:00:00Z',
                    image: null,
                    user_id: 'user-003',
                    community_id: null,
                    user: {
                        id: 'user-003',
                        name: 'Ana López',
                        username: 'anal'
                    },
                    community: null
                }
            ];

            this.renderPosts(mockPosts);
        }
    }

    static renderPosts(posts) {
        const container = document.getElementById('postsList');

        if (posts && posts.length > 0) {
            console.log('🖼️ Rendering posts with images:', posts.map(p => ({ id: p.id, hasImage: !!p.image, imageUrl: p.image })));

            container.innerHTML = posts.map(post => {
                // PostDto structure: id, body, date_created, image, user_id, community_id, user, community
                const body = post.body || 'Sin contenido';
                const userName = post.user?.name || post.user?.username || 'Usuario';
                const communityName = post.community?.name || 'General';
                const dateCreated = post.date_created || new Date().toISOString();
                const postId = post.id || '';

                console.log(`📋 Post ${postId} image:`, post.image);

                // Get first line as title, rest as content
                const lines = body.split('\n');
                const title = lines[0] || 'Post sin título';
                const content = lines.slice(1).join('\n') || body;

                return `
                <div class="post-card">
                    <div class="post-header">
                        <div class="post-info">
                                <div class="post-title">${title}</div>
                            <div class="post-meta">
                                    por ${userName} en ${communityName} 
                                    - ${formatDate(dateCreated)}
                            </div>
                        </div>
                    </div>
                    
                    <div class="post-content">
                            <p>${content}</p>
                            ${post.image ? `<div class="post-image">
                                <img src="${post.image}" 
                                     alt="Imagen del post" 
                                     style="max-width: 100%; height: auto; border-radius: 8px; margin-top: 10px;"
                                     onload="console.log('✅ Image loaded:', '${post.image}')"
                                     onerror="console.error('❌ Image failed to load:', '${post.image}')">
                            </div>` : `<div class="no-image-debug" style="color: red; font-size: 12px;">Debug: No image (${post.image})</div>`}
                    </div>
                    
                    <div class="post-actions">
                            <button class="btn btn-sm btn-secondary" onclick="PostsUI.showPostDetails('${postId}')">
                            <i class="fas fa-comments"></i> Ver Comentarios
                        </button>
                            <button class="btn btn-sm btn-primary" onclick="PostsUI.showCommentForm('${postId}')">
                            <i class="fas fa-reply"></i> Comentar
                        </button>
                    </div>
                </div>
                `;
            }).join('');
        } else {
            container.innerHTML = '<p class="text-center">No hay posts disponibles. ¡Crea el primero!</p>';
        }
    }

    static async showPostDetails(postId) {
        try {
            // First get all posts to find the specific post
            const posts = await BookyAPI.getPosts();
            const post = posts.find(p => p.id === postId);

            const comments = await BookyAPI.getPostComments(postId);

            const modalContent = document.getElementById('postDetailsContent');

            if (post) {
                const body = post.body || 'Sin contenido';
                const userName = post.user?.name || post.user?.username || 'Usuario';
                const communityName = post.community?.name || 'General';
                const dateCreated = post.date_created || new Date().toISOString();

                modalContent.innerHTML = `
                    <div class="post-details-full">
                        <div class="post-header">
                            <h3>${body.split('\n')[0] || 'Post'}</h3>
                            <div class="post-meta">
                                por ${userName} en ${communityName} - ${formatDate(dateCreated)}
                            </div>
                        </div>
                        
                        <div class="post-content-full">
                            <p>${body}</p>
                            ${post.image ? `<div class="post-image-full"><img src="${post.image}" alt="Imagen del post" style="max-width: 100%; height: auto; border-radius: 8px; margin: 15px 0;"></div>` : ''}
                        </div>
                        
                        <hr style="margin: 20px 0;">
                        
                        <h4>💬 Comentarios</h4>
                <div class="comments-section">
                    ${comments && comments.length > 0 ? comments.map(comment => `
                        <div class="comment-item" data-comment-id="${comment.id}">
                            <div class="comment-header">
                                <div class="comment-author">${comment.user?.name || 'Usuario'}</div>
                                ${comment.user_id === currentUser?.id ? `
                                    <button class="btn btn-sm btn-danger comment-delete-btn" onclick="PostsUI.deleteComment('${comment.id}', '${postId}')">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                ` : ''}
                            </div>
                            <div class="comment-content">${comment.body}</div>
                            <small class="text-muted">${formatDate(comment.date_created)}</small>
                        </div>
                    `).join('') : '<p>No hay comentarios aún</p>'}
                </div>
                
                <div class="comment-form">
                    <h4>Agregar Comentario</h4>
                            <textarea id="newCommentContent" placeholder="Escribe tu comentario..." rows="3"></textarea>
                    <button class="btn btn-primary" onclick="PostsUI.addComment('${postId}')">
                        Publicar Comentario
                    </button>
                        </div>
                </div>
            `;
            } else {
                modalContent.innerHTML = '<p>Post no encontrado</p>';
            }

            document.getElementById('postDetailsModal').style.display = 'block';
        } catch (error) {
            showToast('Error cargando detalles del post', 'error');
        }
    }

    static showCommentForm(postId) {
        this.showPostDetails(postId);
    }

    static async addComment(postId) {
        const content = document.getElementById('newCommentContent').value;
        if (!content.trim()) {
            showToast('Escribe un comentario', 'warning');
            return;
        }

        try {
            await BookyAPI.addComment(postId, content);
            showToast('Comentario agregado', 'success');
            await this.showPostDetails(postId); // Refresh comments
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            showToast('Error agregando comentario', 'error');
        }
    }

    static async deleteComment(commentId, postId) {
        // Confirmación antes de eliminar
        if (!confirm('¿Estás seguro de que quieres eliminar este comentario?')) {
            return;
        }

        try {
            console.log('🗑️ Deleting comment:', commentId);
            await BookyAPI.deleteComment(commentId);
            showToast('Comentario eliminado', 'success');
            await this.showPostDetails(postId); // Refresh comments
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            console.error('Error deleting comment:', error);
            if (error.message.includes('403') || error.message.includes('Forbidden')) {
                showToast('No tienes permisos para eliminar este comentario', 'error');
            } else if (error.message.includes('404')) {
                showToast('Comentario no encontrado', 'error');
            } else {
                showToast('Error eliminando comentario', 'error');
            }
        }
    }

    static async createPost(formData) {
        try {
            const postData = {
                community_id: formData.get('communityId') || null,
                body: formData.get('content')
            };

            console.log('Creating post with data:', postData);

            // Get image file if provided
            const imageFile = formData.get('image');

            await BookyAPI.createPost(postData, imageFile);
            showToast('¡Post creado exitosamente!', 'success');
            await this.loadPosts();
            await GamificationUI.updateGamificationPanel(currentUser.id);

            // Close modal
            document.getElementById('createPostModal').style.display = 'none';
        } catch (error) {
            console.error('Error creating post:', error);
            showToast('Error creando el post', 'error');
        }
    }
}

// ===== EXCHANGES MANAGEMENT =====
class ExchangesUI {
    static async loadExchanges() {
        try {
            console.log('🔄 Loading exchanges for current user:', currentUser?.id);

            if (!currentUser?.id) {
                console.error('❌ No current user ID available');
                document.getElementById('exchangesList').innerHTML = '<p class="text-center">Error: Usuario no identificado</p>';
                return;
            }

            const exchanges = await BookyAPI.getUserExchanges(currentUser.id);
            console.log('📋 Exchanges received in loadExchanges:', exchanges);
            console.log('📋 First exchange details:', exchanges[0]);

            if (!Array.isArray(exchanges)) {
                console.warn('⚠️ Exchanges is not an array:', exchanges);
                document.getElementById('exchangesList').innerHTML = '<p class="text-center">No hay intercambios disponibles</p>';
                return;
            }

            this.renderExchanges(exchanges);
        } catch (error) {
            console.error('❌ Error loading exchanges:', error);
            console.error('Error details:', {
                message: error.message,
                status: error.status,
                response: error.response
            });
            document.getElementById('exchangesList').innerHTML = `
                <div class="text-center">
                    <p>Error cargando intercambios</p>
                    <small class="text-muted">Error: ${error.message || 'Error desconocido'}</small>
                </div>
            `;
        }
    }

    static async renderExchanges(exchanges) {
        console.log('🎨 Rendering exchanges:', exchanges);
        const container = document.getElementById('exchangesList');

        if (!container) {
            console.error('❌ exchangesList container not found in DOM');
            return;
        }

        if (exchanges && exchanges.length > 0) {
            console.log(`📋 Rendering ${exchanges.length} exchanges`);

            // Show loading while resolving books
            container.innerHTML = '<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Cargando detalles de intercambios...</div>';

            const exchangeCards = [];
            for (let index = 0; index < exchanges.length; index++) {
                const exchange = exchanges[index];
                console.log(`📝 Processing exchange ${index}:`, exchange);

                const card = await this.renderSingleExchange(exchange);
                exchangeCards.push(card);
            }

            container.innerHTML = exchangeCards.join('');
            console.log('✅ Exchanges rendered successfully');
        } else {
            console.log('📋 No exchanges to render');
            container.innerHTML = '<p class="text-center">No tienes intercambios activos.</p>';
        }
    }

    static async renderSingleExchange(exchange) {
        // Safe data extraction with extensive fallbacks
        const exchangeId = exchange?.id || 'unknown';
        const status = exchange?.status || 'UNKNOWN';
        const dateCreated = exchange?.date_created || new Date().toISOString();

        // User data with fallbacks
        const requester = exchange?.requester || {};
        const owner = exchange?.owner || {};
        const requesterName = requester?.name || requester?.username || 'Solicitante';
        const ownerName = owner?.name || owner?.username || 'Propietario';

        // Determine user roles
        const isCurrentUserOwner = owner?.id === currentUser?.id;
        const isCurrentUserRequester = requester?.id === currentUser?.id;
        const canRespond = status === 'PENDING' && isCurrentUserOwner;

        // Role descriptions
        const requesterRole = isCurrentUserRequester ? 'Tú (Solicitante)' : `${requesterName} (Solicitante)`;
        const ownerRole = isCurrentUserOwner ? 'Tú (Propietario)' : `${ownerName} (Propietario)`;

        // Handle books - use complete book data from backend (in snake_case)
        console.log('📚 Debug: Processing books for exchange:', exchangeId);

        const ownerBooksData = exchange?.owner_books || [];
        const requesterBooksData = exchange?.requester_books || [];

        const ownerBooks = this.extractBookData(ownerBooksData);
        const requesterBooks = this.extractBookData(requesterBooksData);

        console.log('📚 Debug: ownerBooks processed:', ownerBooks);
        console.log('📚 Debug: requesterBooks processed:', requesterBooks);

        return `
            <div class="exchange-card ${isCurrentUserOwner ? 'as-owner' : 'as-requester'}">
                <div class="exchange-header">
                    <div>
                        <h4>🔄 Intercambio #${exchangeId}</h4>
                        <small>📅 ${formatDate ? formatDate(dateCreated) : new Date(dateCreated).toLocaleDateString()}</small>
                    </div>
                    <span class="status-badge ${status.toLowerCase()}">${this.getStatusText(status)}</span>
                </div>
                
                <div class="exchange-participants">
                    <div class="participant ${isCurrentUserRequester ? 'current-user' : ''}">
                        ${typeof createUserAvatar === 'function' ? createUserAvatar(requester, 'small') : '👤'}
                        <div class="participant-info">
                            <span class="participant-name">${requesterRole}</span>
                            <small class="participant-role">Solicita libros</small>
                        </div>
                    </div>
                    <div class="exchange-arrow">
                        <span class="arrow-icon">⇄</span>
                        <small>intercambio</small>
                    </div>
                    <div class="participant ${isCurrentUserOwner ? 'current-user' : ''}">
                        ${typeof createUserAvatar === 'function' ? createUserAvatar(owner, 'small') : '👤'}
                        <div class="participant-info">
                            <span class="participant-name">${ownerRole}</span>
                            <small class="participant-role">Propietario de libros</small>
                        </div>
                    </div>
                </div>
                
                <div class="exchange-books">
                    <div class="book-list">
                        <h5>📚 ${requesterRole} quiere:</h5>
                        <div class="books-grid">
                            ${ownerBooks.length > 0 ? ownerBooks.map(book => this.renderBookCard(book)).join('') : '<p class="no-books">No hay libros especificados</p>'}
                        </div>
                    </div>
                    
                    <div class="book-list">
                        <h5>📖 ${requesterRole} ofrece:</h5>
                        <div class="books-grid">
                            ${requesterBooks.length > 0 ? requesterBooks.map(book => this.renderBookCard(book)).join('') : '<p class="no-books">No hay libros especificados</p>'}
                        </div>
                    </div>
                </div>
                
                ${exchange?.message ? `
                    <div class="exchange-message">
                        💬 "${exchange.message}"
                    </div>
                ` : ''}
                
                <div class="exchange-actions">
                    <button class="btn btn-sm btn-outline-primary" onclick="ExchangesUI.showExchangeDetails('${exchangeId}')">
                        🔍 Ver Detalles
                    </button>
                    ${canRespond ? `
                        <button class="btn btn-sm btn-success" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'ACCEPTED')">
                            ✅ Aceptar
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'REJECTED')">
                            ❌ Rechazar
                        </button>
                    ` : ''}
                    ${status === 'PENDING' && isCurrentUserRequester ? `
                        <button class="btn btn-sm btn-outline-danger" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'CANCELLED')">
                            🚫 Cancelar
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    }

    static async resolveBooks(booksData) {
        console.log('🔍 Resolving books:', booksData);

        if (!Array.isArray(booksData) || booksData.length === 0) {
            return [];
        }

        // If already book objects, return as is
        if (booksData[0] && typeof booksData[0] === 'object' && booksData[0].title) {
            console.log('📚 Books already resolved');
            return booksData;
        }

        // If they are IDs, fetch the book details
        const books = [];
        for (const bookId of booksData) {
            try {
                // Try to get book by ID (assuming it's a Book.id)
                const book = await BookyAPI.getBookById(bookId);
                books.push(book);
            } catch (error) {
                console.warn(`⚠️ Could not resolve book ID ${bookId}:`, error);
                // Add placeholder book
                books.push({
                    id: bookId,
                    title: 'Libro no encontrado',
                    author: 'Autor desconocido'
                });
            }
        }

        console.log('📚 Resolved books:', books);
        return books;
    }

    static extractBookData(booksData) {
        console.log('📖 DEBUG: Extracting book data:', booksData);
        console.log('📖 DEBUG: Is array?', Array.isArray(booksData));
        console.log('📖 DEBUG: Length:', booksData?.length);

        if (!Array.isArray(booksData) || booksData.length === 0) {
            console.log('📖 DEBUG: Returning empty array - no data');
            return [];
        }

        return booksData.map(userBook => {
            // If it's a UserBook object, extract the book data
            if (userBook && userBook.book) {
                return {
                    id: userBook.book.id,
                    title: userBook.book.title,
                    author: userBook.book.author,
                    image: userBook.book.image,
                    pages: userBook.book.pages,
                    rate: userBook.book.rate,
                    overview: userBook.book.overview,
                    status: userBook.status,
                    favorite: userBook.favorite,
                    wantsToExchange: userBook.wantsToExchange
                };
            }
            // If it's already a book object
            else if (userBook && userBook.title) {
                return userBook;
            }
            // Fallback for incomplete data
            else {
                return {
                    id: userBook?.id || 'unknown',
                    title: 'Libro no disponible',
                    author: 'Autor desconocido',
                    image: null
                };
            }
        });
    }

    static renderBookCard(book) {
        if (!book) return '';

        const title = book.title || 'Sin título';
        const author = book.author || 'Sin autor';
        const image = book.image || 'https://via.placeholder.com/150x200?text=Sin+Imagen';
        const pages = book.pages ? `${book.pages} pág.` : '';
        const rate = book.rate ? `⭐ ${book.rate}/10` : '';
        const overview = book.overview ? book.overview.substring(0, 100) + '...' : '';

        return `
            <div class="book-card-mini">
                <div class="book-image">
                    <img src="${image}" alt="${title}" onerror="this.src='https://via.placeholder.com/80x120?text=Sin+Imagen'">
                </div>
                <div class="book-info">
                    <h6 class="book-title">${title}</h6>
                    <p class="book-author">${author}</p>
                    ${pages ? `<small class="book-pages">${pages}</small>` : ''}
                    ${rate ? `<small class="book-rate">${rate}</small>` : ''}
                    ${overview ? `<p class="book-overview">${overview}</p>` : ''}
                </div>
            </div>
        `;
    }

    static getStatusText(status) {
        const statusMap = {
            'PENDING': 'Pendiente',
            'ACCEPTED': 'Aceptado',
            'REJECTED': 'Rechazado',
            'COMPLETED': 'Completado',
            'CANCELLED': 'Cancelado'
        };
        return statusMap[status] || status;
    }

    static async proposeExchange(ownerId = null, ownerBookId = null) {
        // Start the new exchange flow
        this.startExchangeWizard();
    }

    static startExchangeWizard() {
        console.log('🚀 Starting exchange wizard');
        this.exchangeData = {
            step: 1,
            selectedBooks: [],
            selectedUsers: [],
            myBooksToOffer: []
        };
        console.log('📊 Initial exchange data:', this.exchangeData);

        this.showExchangeStep1();
    }

    // PASO 1: Buscar libros que quiero
    static showExchangeStep1() {
        const modalContent = `
            <div class="modal" id="exchangeModal" style="display: block;">
                <div class="modal-content exchange-wizard">
                    <span class="close" onclick="ExchangesUI.closeExchangeWizard()">&times;</span>
                    <div class="wizard-header">
                        <h3>💱 Crear Intercambio - Paso 1/4</h3>
                        <div class="wizard-progress">
                            <div class="step active">1</div>
                            <div class="step">2</div>
                            <div class="step">3</div>
                            <div class="step">4</div>
                        </div>
                    </div>
                    
                    <div class="wizard-content">
                        <h4>🔍 Buscar libros que quiero</h4>
                        <p>Busca y selecciona los libros que te interesan obtener:</p>
                        
                        <div class="search-section">
                            <input type="text" id="bookSearchInput" placeholder="Buscar libros..." class="form-control">
                            <button type="button" onclick="ExchangesUI.searchBooksForExchange()" class="btn btn-primary">
                                🔍 Buscar
                            </button>
                        </div>
                        
                        <div id="searchResults" class="search-results"></div>
                        
                        <div class="selected-books">
                            <h5>📚 Libros seleccionados (${this.exchangeData.selectedBooks.length}):</h5>
                            <div id="selectedBooksList" class="selected-items">
                                ${this.renderSelectedBooks()}
                            </div>
                        </div>
                    </div>
                    
                    <div class="wizard-actions">
                        <button type="button" class="btn btn-secondary" onclick="ExchangesUI.closeExchangeWizard()">
                            Cancelar
                        </button>
                        <button type="button" class="btn btn-primary" 
                                onclick="ExchangesUI.goToStep2()" 
                                ${this.exchangeData.selectedBooks.length === 0 ? 'disabled' : ''}>
                            Siguiente →
                        </button>
                    </div>
                </div>
            </div>
        `;

        this.getModalContainer().innerHTML = modalContent;

        // Add enter key handler for search
        document.getElementById('bookSearchInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.searchBooksForExchange();
            }
        });
    }

    // Funciones auxiliares del wizard
    static getModalContainer() {
        return document.getElementById('exchangeModalContainer') || (() => {
            const container = document.createElement('div');
            container.id = 'exchangeModalContainer';
            document.body.appendChild(container);
            return container;
        })();
    }

    static closeExchangeWizard() {
        const modal = document.getElementById('exchangeModal');
        if (modal) {
            modal.style.display = 'none';
        }
        this.exchangeData = null;
    }

    static renderSelectedBooks() {
        if (this.exchangeData.selectedBooks.length === 0) {
            return '<p class="text-muted">No hay libros seleccionados</p>';
        }

        return this.exchangeData.selectedBooks.map(book => `
            <div class="selected-item">
                <span><strong>${book.title}</strong> - ${book.author}</span>
                <button type="button" class="btn btn-sm btn-danger" onclick="ExchangesUI.removeSelectedBook('${book.id}')">
                    ❌
                </button>
            </div>
        `).join('');
    }

    static async searchBooksForExchange() {
        const query = document.getElementById('bookSearchInput').value.trim();
        if (!query) {
            showToast('Ingresa un término de búsqueda', 'warning');
            return;
        }

        try {
            const books = await BookyAPI.searchBooks(query);
            this.renderBookSearchResults(books);
        } catch (error) {
            console.error('Error searching books:', error);
            showToast('Error buscando libros', 'error');
        }
    }

    static renderBookSearchResults(books) {
        const container = document.getElementById('searchResults');

        if (books.length === 0) {
            container.innerHTML = '<p class="text-muted">No se encontraron libros</p>';
            return;
        }

        container.innerHTML = `
            <h5>Resultados de búsqueda:</h5>
            <div class="books-grid">
                ${books.map(book => `
                    <div class="book-card ${this.isBookSelected(book.id) ? 'selected' : ''}">
                        <div class="book-info">
                            <strong>${book.title}</strong><br>
                            <small>por ${book.author}</small><br>
                            <small>ISBN: ${book.isbn}</small>
                        </div>
                        <button type="button" class="btn btn-sm ${this.isBookSelected(book.id) ? 'btn-success' : 'btn-primary'}" 
                                onclick="ExchangesUI.toggleBookSelection('${book.id}', '${book.title}', '${book.author}')">
                            ${this.isBookSelected(book.id) ? '✓ Seleccionado' : '+ Seleccionar'}
                        </button>
                    </div>
                `).join('')}
            </div>
        `;
    }

    static isBookSelected(bookId) {
        return this.exchangeData.selectedBooks.some(book => book.id === bookId);
    }

    static toggleBookSelection(bookId, title, author) {
        console.log('Toggling book selection:', { bookId, title, author });

        const index = this.exchangeData.selectedBooks.findIndex(book => book.id === bookId);

        if (index > -1) {
            // Remove book
            this.exchangeData.selectedBooks.splice(index, 1);
            console.log('Removed book from selection');
        } else {
            // Add book
            const bookToAdd = { id: bookId, title, author };
            this.exchangeData.selectedBooks.push(bookToAdd);
            console.log('Added book to selection:', bookToAdd);
        }

        console.log('Current selected books:', this.exchangeData.selectedBooks);

        // Refresh the UI
        this.showExchangeStep1();
    }

    static removeSelectedBook(bookId) {
        this.exchangeData.selectedBooks = this.exchangeData.selectedBooks.filter(book => book.id !== bookId);
        this.showExchangeStep1();
    }

    // PASO 2: Buscar usuarios que tienen esos libros
    static async goToStep2() {
        if (this.exchangeData.selectedBooks.length === 0) {
            showToast('Selecciona al menos un libro', 'warning');
            return;
        }

        try {
            const bookIds = this.exchangeData.selectedBooks.map(book => book.id);
            console.log('Selected books for step 2:', this.exchangeData.selectedBooks);
            console.log('Book IDs to search:', bookIds);

            if (bookIds.length === 0 || bookIds.some(id => !id)) {
                throw new Error('Invalid book IDs detected');
            }

            const users = await BookyAPI.searchUsersByBooks(bookIds);
            console.log('👥 Users found (raw):', users);

            // Filter out current user to prevent self-exchange
            const filteredUsers = users.filter(user => user.id !== currentUser.id);
            console.log('👥 Users filtered (excluding self):', filteredUsers);

            this.exchangeData.step = 2;
            this.showExchangeStep2(filteredUsers);
        } catch (error) {
            console.error('Error searching users by books:', error);
            showToast(`Error buscando usuarios: ${error.message}`, 'error');
        }
    }

    static showExchangeStep2(users) {
        const modalContent = `
            <div class="modal" id="exchangeModal" style="display: block;">
                <div class="modal-content exchange-wizard">
                    <span class="close" onclick="ExchangesUI.closeExchangeWizard()">&times;</span>
                    <div class="wizard-header">
                        <h3>💱 Crear Intercambio - Paso 2/4</h3>
                        <div class="wizard-progress">
                            <div class="step completed">1</div>
                            <div class="step active">2</div>
                            <div class="step">3</div>
                            <div class="step">4</div>
                        </div>
                    </div>
                    
                    <div class="wizard-content">
                        <h4>👥 Usuarios que tienen estos libros</h4>
                        <p>Selecciona con qué usuarios quieres intercambiar:</p>
                        
                        <div class="books-summary">
                            <strong>Libros solicitados:</strong>
                            ${this.exchangeData.selectedBooks.map(book => `<span class="book-tag">${book.title}</span>`).join('')}
                        </div>
                        
                        <div class="users-results">
                            ${users.length > 0 ? `
                                <h5>Usuarios encontrados (${users.length}):</h5>
                                <div class="users-grid">
                                    ${users.map(user => `
                                        <div class="user-card ${this.isUserSelected(user.id) ? 'selected' : ''}">
                                            ${this.createUserAvatar(user)}
                                            <div class="user-info">
                                                <strong>${user.name} ${user.lastname}</strong><br>
                                                <small>@${user.username}</small>
                                            </div>
                                            <button type="button" class="btn btn-sm ${this.isUserSelected(user.id) ? 'btn-success' : 'btn-primary'}" 
                                                    onclick="ExchangesUI.toggleUserSelection('${user.id}', '${user.name}', '${user.lastname}', '${user.username}')">
                                                ${this.isUserSelected(user.id) ? '✓ Seleccionado' : '+ Seleccionar'}
                                            </button>
                                        </div>
                                    `).join('')}
                                </div>
                            ` : `
                                <div class="no-results">
                                    <p>😔 No se encontraron usuarios que tengan todos estos libros disponibles para intercambio.</p>
                                    <p>Intenta con menos libros o libros diferentes.</p>
                                </div>
                            `}
                        </div>
                    </div>
                    
                    <div class="wizard-actions">
                        <button type="button" class="btn btn-secondary" onclick="ExchangesUI.goToStep1()">
                            ← Anterior
                        </button>
                        <button type="button" class="btn btn-primary" 
                                onclick="ExchangesUI.goToStep3()" 
                                ${this.exchangeData.selectedUsers.length === 0 ? 'disabled' : ''}>
                            Siguiente →
                        </button>
                    </div>
                </div>
            </div>
        `;

        this.getModalContainer().innerHTML = modalContent;
    }

    static createUserAvatar(user) {
        const initials = (user.name?.charAt(0) || '') + (user.lastname?.charAt(0) || '');
        return `<div class="user-avatar">${initials}</div>`;
    }

    static isUserSelected(userId) {
        return this.exchangeData.selectedUsers.some(user => user.id === userId);
    }

    static toggleUserSelection(userId, name, lastname, username) {
        console.log('🔄 Toggling user selection:', { userId, name, lastname, username });
        console.log('📊 Current exchangeData before toggle:', this.exchangeData);

        const index = this.exchangeData.selectedUsers.findIndex(user => user.id === userId);

        if (index > -1) {
            // Remove user
            this.exchangeData.selectedUsers.splice(index, 1);
            console.log('📤 Removed user from selection:', userId);
        } else {
            // Add user
            this.exchangeData.selectedUsers.push({ id: userId, name, lastname, username });
            console.log('📥 Added user to selection:', { id: userId, name, lastname, username });
        }

        console.log('📊 selectedUsers after toggle:', this.exchangeData.selectedUsers);

        // Refresh the button state
        const users = this.exchangeData.selectedUsers; // This would need to be passed back
        this.showExchangeStep2(users);
    }

    static goToStep1() {
        this.exchangeData.step = 1;
        this.showExchangeStep1();
    }

    // PASO 3: Seleccionar mis libros para ofrecer
    static async goToStep3() {
        if (this.exchangeData.selectedUsers.length === 0) {
            showToast('Selecciona al menos un usuario', 'warning');
            return;
        }

        try {
            // Use the new filter to get only books available for exchange
            const availableBooks = await BookyAPI.getUserBooks(currentUser.id, { wantsToExchange: true });

            if (availableBooks.length === 0) {
                showToast('No tienes libros disponibles para intercambio. Marca algunos como "Intercambiables" primero.', 'warning');
                return;
            }

            this.exchangeData.step = 3;
            this.showExchangeStep3(availableBooks);
        } catch (error) {
            console.error('Error loading my books:', error);
            showToast('Error cargando tus libros', 'error');
        }
    }

    static showExchangeStep3(myBooks) {
        const modalContent = `
            <div class="modal" id="exchangeModal" style="display: block;">
                <div class="modal-content exchange-wizard">
                    <span class="close" onclick="ExchangesUI.closeExchangeWizard()">&times;</span>
                    <div class="wizard-header">
                        <h3>💱 Crear Intercambio - Paso 3/4</h3>
                        <div class="wizard-progress">
                            <div class="step completed">1</div>
                            <div class="step completed">2</div>
                            <div class="step active">3</div>
                            <div class="step">4</div>
                        </div>
                    </div>
                    
                    <div class="wizard-content">
                        <h4>📚 Selecciona tus libros para ofrecer</h4>
                        <p>Elige qué libros quieres ofrecer a cambio:</p>
                        
                        <div class="exchange-summary">
                            <div class="summary-section">
                                <strong>Quiero obtener:</strong>
                                ${this.exchangeData.selectedBooks.map(book => `<span class="book-tag">${book.title}</span>`).join('')}
                            </div>
                            <div class="summary-section">
                                <strong>De usuarios:</strong>
                                ${this.exchangeData.selectedUsers.map(user => `<span class="user-tag">${user.name} ${user.lastname}</span>`).join('')}
                            </div>
                        </div>
                        
                        <div class="my-books">
                            <h5>Mis libros disponibles (${myBooks.length}):</h5>
                            <div class="books-grid">
                                ${myBooks.map(book => `
                                    <div class="book-card ${this.isMyBookSelected(book.id) ? 'selected' : ''}">
                                        <div class="book-info">
                                            <strong>${book.book?.title || 'Sin título'}</strong><br>
                                            <small>por ${book.book?.author || 'Autor desconocido'}</small><br>
                                            <small>Estado: ${BooksUI.getStatusText(book.status)}</small>
                                        </div>
                                        <button type="button" class="btn btn-sm ${this.isMyBookSelected(book.id) ? 'btn-success' : 'btn-primary'}" 
                                                onclick="ExchangesUI.toggleMyBookSelection('${book.id}', '${book.book?.id}', '${book.book?.title}', '${book.book?.author}')">
                                            ${this.isMyBookSelected(book.id) ? '✓ Seleccionado' : '+ Seleccionar'}
                                        </button>
                                    </div>
                                `).join('')}
                            </div>
                        </div>

                        <div class="selected-books">
                            <h5>📦 Libros a ofrecer (${this.exchangeData.myBooksToOffer.length}):</h5>
                            <div class="selected-items">
                                ${this.renderMySelectedBooks()}
                            </div>
                        </div>
                    </div>
                    
                    <div class="wizard-actions">
                        <button type="button" class="btn btn-secondary" onclick="ExchangesUI.goToStep2Previous()">
                            ← Anterior
                        </button>
                        <button type="button" class="btn btn-primary" 
                                onclick="ExchangesUI.goToStep4()" 
                                ${this.exchangeData.myBooksToOffer.length === 0 ? 'disabled' : ''}>
                            Siguiente →
                        </button>
                    </div>
                </div>
            </div>
        `;

        this.getModalContainer().innerHTML = modalContent;
    }

    static isMyBookSelected(userBookId) {
        return this.exchangeData.myBooksToOffer.some(book => book.id === userBookId);
    }

    static toggleMyBookSelection(userBookId, bookId, title, author) {
        console.log('🔄 Toggling book selection:', { userBookId, bookId, title, author });

        const index = this.exchangeData.myBooksToOffer.findIndex(book => book.id === userBookId);

        if (index > -1) {
            // Remove book
            this.exchangeData.myBooksToOffer.splice(index, 1);
            console.log('📤 Removed book from selection');
        } else {
            // Add book - store both UserBook ID and Book ID
            this.exchangeData.myBooksToOffer.push({
                id: userBookId,    // UserBook.id  
                bookId: bookId,    // Book.id (needed for backend validation)
                title,
                author
            });
            console.log('📥 Added book to selection');
        }

        console.log('📋 Current myBooksToOffer:', this.exchangeData.myBooksToOffer);

        // Refresh the UI
        this.goToStep3();
    }

    static renderMySelectedBooks() {
        if (this.exchangeData.myBooksToOffer.length === 0) {
            return '<p class="text-muted">No hay libros seleccionados para ofrecer</p>';
        }

        return this.exchangeData.myBooksToOffer.map(book => `
            <div class="selected-item">
                <span><strong>${book.title}</strong> - ${book.author}</span>
                <button type="button" class="btn btn-sm btn-danger" onclick="ExchangesUI.removeMySelectedBook('${book.id}')">
                    ❌
                </button>
            </div>
        `).join('');
    }

    static removeMySelectedBook(bookId) {
        this.exchangeData.myBooksToOffer = this.exchangeData.myBooksToOffer.filter(book => book.id !== bookId);
        this.goToStep3();
    }

    static async goToStep2Previous() {
        // Go back to step 2 with the users we found before
        const bookIds = this.exchangeData.selectedBooks.map(book => book.id);
        const users = await BookyAPI.searchUsersByBooks(bookIds);
        this.exchangeData.step = 2;
        this.showExchangeStep2(users);
    }

    // PASO 4: Resumen y confirmación
    static goToStep4() {
        if (this.exchangeData.myBooksToOffer.length === 0) {
            showToast('Selecciona al menos un libro para ofrecer', 'warning');
            return;
        }

        this.exchangeData.step = 4;
        this.showExchangeStep4();
    }

    static showExchangeStep4() {
        const modalContent = `
            <div class="modal" id="exchangeModal" style="display: block;">
                <div class="modal-content exchange-wizard">
                    <span class="close" onclick="ExchangesUI.closeExchangeWizard()">&times;</span>
                    <div class="wizard-header">
                        <h3>💱 Crear Intercambio - Paso 4/4</h3>
                        <div class="wizard-progress">
                            <div class="step completed">1</div>
                            <div class="step completed">2</div>
                            <div class="step completed">3</div>
                            <div class="step active">4</div>
                        </div>
                    </div>
                    
                    <div class="wizard-content">
                        <h4>✅ Confirmar intercambio</h4>
                        <p>Revisa tu propuesta antes de enviarla:</p>
                        
                        <div class="exchange-final-summary">
                            <div class="summary-card">
                                <h5>📖 Quiero recibir:</h5>
                                <ul>
                                    ${this.exchangeData.selectedBooks.map(book => `
                                        <li><strong>${book.title}</strong> - ${book.author}</li>
                                    `).join('')}
                                </ul>
                            </div>

                            <div class="summary-card">
                                <h5>👥 De usuarios:</h5>
                                <ul>
                                    ${this.exchangeData.selectedUsers.map(user => `
                                        <li><strong>${user.name} ${user.lastname}</strong> (@${user.username})</li>
                                    `).join('')}
                                </ul>
                            </div>

                            <div class="summary-card">
                                <h5>📦 Voy a ofrecer:</h5>
                                <ul>
                                    ${this.exchangeData.myBooksToOffer.map(book => `
                                        <li><strong>${book.title}</strong> - ${book.author}</li>
                                    `).join('')}
                                </ul>
                            </div>
                        </div>
                        
                        <div class="note">
                            <p><strong>Nota:</strong> Se crearán propuestas de intercambio individuales para cada usuario seleccionado.</p>
                        </div>
                    </div>
                    
                    <div class="wizard-actions">
                        <button type="button" class="btn btn-secondary" onclick="ExchangesUI.goToStep3Previous()">
                            ← Anterior
                        </button>
                        <button type="button" class="btn btn-success" onclick="ExchangesUI.finalizeExchange()">
                            🚀 Enviar Propuestas
                        </button>
                    </div>
                </div>
            </div>
        `;

        this.getModalContainer().innerHTML = modalContent;
    }

    static async goToStep3Previous() {
        const availableBooks = await BookyAPI.getUserBooks(currentUser.id, { wantsToExchange: true });
        this.exchangeData.step = 3;
        this.showExchangeStep3(availableBooks);
    }

    // Finalizar y enviar propuestas
    static async finalizeExchange() {
        try {
            console.log('🚀 Starting finalizeExchange');
            console.log('📊 Exchange data state:', this.exchangeData);

            // Validate required data
            if (!this.exchangeData.selectedUsers || this.exchangeData.selectedUsers.length === 0) {
                showToast('No hay usuarios seleccionados', 'error');
                return;
            }

            if (!this.exchangeData.selectedBooks || this.exchangeData.selectedBooks.length === 0) {
                showToast('No hay libros seleccionados para solicitar', 'error');
                return;
            }

            if (!this.exchangeData.myBooksToOffer || this.exchangeData.myBooksToOffer.length === 0) {
                showToast('No hay libros seleccionados para ofrecer', 'error');
                return;
            }

            if (!currentUser || !currentUser.id) {
                console.error('❌ Current user is null or missing ID:', currentUser);
                showToast('Usuario no autenticado', 'error');
                return;
            }

            console.log('✅ Current user validated:', currentUser);

            const proposals = [];

            // Create one exchange proposal per user
            for (const user of this.exchangeData.selectedUsers) {
                console.log('📋 Processing exchange for user:', user);
                console.log('📋 Selected books (what we want):', this.exchangeData.selectedBooks);
                console.log('📋 My books to offer:', this.exchangeData.myBooksToOffer);
                console.log('🔍 Current user (requester):', currentUser);
                console.log('🔍 Target user (owner):', user);
                console.log('⚠️  Are they the same user?', user.id === currentUser.id);

                // Skip if trying to create exchange with self
                if (user.id === currentUser.id) {
                    console.warn('⚠️  Skipping exchange with self for user:', user.id);
                    proposals.push({ user: user.name, success: false, error: 'Cannot create exchange with yourself' });
                    continue;
                }

                // Get the user's books to find the UserBook IDs for the books we want
                try {
                    const userBooks = await BookyAPI.getUserBooks(user.id);
                    console.log('User books for', user.name, ':', userBooks);

                    // Find the Book IDs for the books we want from this user
                    const ownerBookIds = [];
                    for (const selectedBook of this.exchangeData.selectedBooks) {
                        const userBook = userBooks.find(ub => ub.book && ub.book.id === selectedBook.id);
                        if (userBook) {
                            ownerBookIds.push(userBook.book.id); // This is the Book ID (what backend expects)
                        }
                    }

                    if (ownerBookIds.length === 0) {
                        console.warn(`No matching books found for user ${user.name}`);
                        proposals.push({ user: user.name, success: false, error: 'No matching books found' });
                        continue;
                    }

                    // Get the Book IDs for my offered books (backend expects Book.id, not UserBook.id)
                    const requesterBookIds = this.exchangeData.myBooksToOffer.map(book => book.bookId);

                    console.log('🔍 Building exchange data:');
                    console.log('  - user.id:', user.id);
                    console.log('  - currentUser.id:', currentUser.id);
                    console.log('  - ownerBookIds:', ownerBookIds);
                    console.log('  - requesterBookIds:', requesterBookIds);
                    console.log('  - myBooksToOffer array:', this.exchangeData.myBooksToOffer);

                    const exchangeData = {
                        owner_id: user.id,          // snake_case
                        requester_id: currentUser.id,   // snake_case
                        owner_book_ids: ownerBookIds,   // snake_case
                        requester_book_ids: requesterBookIds   // snake_case
                    };

                    console.log('📤 Final exchange data to send:', exchangeData);

                    // Additional validation before sending
                    if (!exchangeData.owner_id) {
                        throw new Error('owner_id is null or undefined');
                    }
                    if (!exchangeData.requester_id) {
                        throw new Error('requester_id is null or undefined');
                    }
                    if (!exchangeData.owner_book_ids || exchangeData.owner_book_ids.length === 0) {
                        throw new Error('owner_book_ids is null, undefined, or empty');
                    }
                    if (!exchangeData.requester_book_ids || exchangeData.requester_book_ids.length === 0) {
                        throw new Error('requester_book_ids is null, undefined, or empty');
                    }

                    const result = await BookyAPI.createExchange(exchangeData);
                    proposals.push({ user: user.name, success: true, result });
                } catch (error) {
                    console.error('Error creating exchange for user:', user.name, error);
                    proposals.push({ user: user.name, success: false, error: error.message });
                }
            }

            // Show results
            const successCount = proposals.filter(p => p.success).length;
            const failCount = proposals.filter(p => !p.success).length;

            if (successCount > 0) {
                showToast(`¡${successCount} propuesta(s) de intercambio enviada(s) exitosamente!`, 'success');
            }

            if (failCount > 0) {
                showToast(`${failCount} propuesta(s) fallaron`, 'error');
            }

            // Close wizard and refresh exchanges
            this.closeExchangeWizard();
            await this.loadExchanges();
            await GamificationUI.updateGamificationPanel(currentUser.id);

        } catch (error) {
            console.error('Error finalizing exchange:', error);
            showToast('Error enviando propuestas', 'error');
        }
    }

    static async submitExchange(ownerId, ownerBookId, form) {
        try {
            const formData = new FormData(form);
            const selectedBooks = formData.getAll('myBooks');

            if (selectedBooks.length === 0) {
                showToast('Selecciona al menos un libro para ofrecer', 'warning');
                return;
            }

            const exchangeData = {
                ownerId: ownerId,
                requesterId: currentUser.id,
                ownerBookIds: [ownerBookId],
                requesterBookIds: selectedBooks
            };

            console.log('Creating exchange:', exchangeData);
            await BookyAPI.createExchange(exchangeData);

            showToast('¡Propuesta de intercambio enviada!', 'success');
            document.getElementById('exchangeModal').style.display = 'none';

            // Refresh exchanges list if we're on that tab
            await this.loadExchanges();

        } catch (error) {
            console.error('Error creating exchange:', error);
            showToast('Error al crear el intercambio', 'error');
        }
    }

    static async respondToExchange(exchangeId, response) {
        try {
            await BookyAPI.respondToExchange(exchangeId, response);
            showToast(`Intercambio ${response === 'ACCEPTED' ? 'aceptado' : 'rechazado'}`, 'success');
            await this.loadExchanges();
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            showToast('Error procesando respuesta', 'error');
        }
    }

    static async showExchangeDetails(exchangeId) {
        try {
            console.log('🔍 Loading exchange details for:', exchangeId);

            // Get the exchange details from the API
            const exchange = await BookyAPI.getExchangeById(exchangeId);
            console.log('📋 Exchange details loaded:', exchange);

            this.renderExchangeDetailsModal(exchange);
        } catch (error) {
            console.error('❌ Error loading exchange details:', error);
            showToast('Error cargando detalles del intercambio', 'error');
        }
    }

    static renderExchangeDetailsModal(exchange) {
        const exchangeId = exchange?.id || 'unknown';
        const status = exchange?.status || 'UNKNOWN';
        const dateCreated = exchange?.date_created || new Date().toISOString();

        // User data
        const requester = exchange?.requester || {};
        const owner = exchange?.owner || {};
        const requesterName = requester?.name || requester?.username || 'Solicitante';
        const ownerName = owner?.name || owner?.username || 'Propietario';

        // Role determination
        const isCurrentUserOwner = owner?.id === currentUser?.id;
        const isCurrentUserRequester = requester?.id === currentUser?.id;
        const requesterRole = isCurrentUserRequester ? 'Tú' : requesterName;
        const ownerRole = isCurrentUserOwner ? 'Tú' : ownerName;

        // Books
        const ownerBooks = exchange?.ownerBooks || [];
        const requesterBooks = exchange?.requesterBooks || [];

        const modalContent = `
            <div class="modal" id="exchangeDetailsModal" style="display: block;">
                <div class="modal-content exchange-details-modal">
                    <div class="modal-header">
                        <h3>🔍 Detalles del Intercambio #${exchangeId}</h3>
                        <button type="button" class="btn btn-close" onclick="ExchangesUI.closeExchangeDetailsModal()">×</button>
                    </div>
                    
                    <div class="modal-body">
                        <!-- Status and Date -->
                        <div class="exchange-overview">
                            <div class="overview-item">
                                <strong>Estado:</strong>
                                <span class="status-badge ${status.toLowerCase()}">${this.getStatusText(status)}</span>
                            </div>
                            <div class="overview-item">
                                <strong>Fecha de creación:</strong>
                                <span>📅 ${new Date(dateCreated).toLocaleDateString()} - ${new Date(dateCreated).toLocaleTimeString()}</span>
                            </div>
                        </div>
                        
                        <!-- Participants -->
                        <div class="participants-detail">
                            <h4>👥 Participantes</h4>
                            <div class="participants-grid">
                                <div class="participant-card ${isCurrentUserRequester ? 'current-user' : ''}">
                                    ${typeof createUserAvatar === 'function' ? createUserAvatar(requester, 'medium') : '👤'}
                                    <div class="participant-details">
                                        <h5>${requesterRole}</h5>
                                        <p class="role-description">🔍 Solicitante - Busca libros</p>
                                        ${requester.email ? `<small>📧 ${requester.email}</small>` : ''}
                                    </div>
                                </div>
                                
                                <div class="participant-card ${isCurrentUserOwner ? 'current-user' : ''}">
                                    ${typeof createUserAvatar === 'function' ? createUserAvatar(owner, 'medium') : '👤'}
                                    <div class="participant-details">
                                        <h5>${ownerRole}</h5>
                                        <p class="role-description">📚 Propietario - Tiene los libros</p>
                                        ${owner.email ? `<small>📧 ${owner.email}</small>` : ''}
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Books Exchange Details -->
                        <div class="books-detail">
                            <h4>📖 Detalles del Intercambio</h4>
                            
                            <div class="books-exchange-flow">
                                <div class="book-section">
                                    <h5>📚 ${requesterRole} quiere obtener:</h5>
                                    <div class="books-list">
                                        ${ownerBooks.length > 0 ? ownerBooks.map(book => `
                                            <div class="book-detail-item">
                                                <div class="book-info">
                                                    <strong>${book?.book?.title || book?.title || 'Sin título'}</strong>
                                                    <span class="book-author">por ${book?.book?.author || book?.author || 'Sin autor'}</span>
                                                </div>
                                                ${book?.book?.image ? `<img src="${book.book.image}" alt="${book?.book?.title}" class="book-thumbnail">` : '📖'}
                                            </div>
                                        `).join('') : '<p class="no-books">No hay libros especificados</p>'}
                                    </div>
                                </div>
                                
                                <div class="exchange-flow-arrow">
                                    <span>⇅</span>
                                    <small>a cambio de</small>
                                </div>
                                
                                <div class="book-section">
                                    <h5>📖 ${requesterRole} ofrece:</h5>
                                    <div class="books-list">
                                        ${requesterBooks.length > 0 ? requesterBooks.map(book => `
                                            <div class="book-detail-item">
                                                <div class="book-info">
                                                    <strong>${book?.book?.title || book?.title || 'Sin título'}</strong>
                                                    <span class="book-author">por ${book?.book?.author || book?.author || 'Sin autor'}</span>
                                                </div>
                                                ${book?.book?.image ? `<img src="${book.book.image}" alt="${book?.book?.title}" class="book-thumbnail">` : '📖'}
                                            </div>
                                        `).join('') : '<p class="no-books">No hay libros especificados</p>'}
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        ${exchange?.message ? `
                            <div class="exchange-message-detail">
                                <h4>💬 Mensaje</h4>
                                <div class="message-content">
                                    "${exchange.message}"
                                </div>
                            </div>
                        ` : ''}
                        
                        <!-- Action Buttons -->
                        <div class="modal-actions">
                            ${status === 'PENDING' && isCurrentUserOwner ? `
                                <button class="btn btn-success" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'ACCEPTED')">
                                    ✅ Aceptar Intercambio
                                </button>
                                <button class="btn btn-danger" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'REJECTED')">
                                    ❌ Rechazar Intercambio
                                </button>
                            ` : ''}
                            ${status === 'PENDING' && isCurrentUserRequester ? `
                                <button class="btn btn-outline-danger" onclick="ExchangesUI.respondToExchange('${exchangeId}', 'CANCELLED')">
                                    🚫 Cancelar Intercambio
                                </button>
                            ` : ''}
                            <button class="btn btn-secondary" onclick="ExchangesUI.closeExchangeDetailsModal()">
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalContent);
    }

    static closeExchangeDetailsModal() {
        const modal = document.getElementById('exchangeDetailsModal');
        if (modal) {
            modal.remove();
        }
    }

    // DEBUG: Test function to manually create an exchange
    static async testExchange() {
        console.log('🧪 Testing manual exchange creation');

        // Simulate test data
        const testExchangeData = {
            owner_id: 'user-002',  // Some test user ID
            requester_id: currentUser?.id || 'user-001',  // Current user
            owner_book_ids: ['userbook-789'],  // Some test UserBook ID
            requester_book_ids: ['userbook-123']  // Some test UserBook ID
        };

        console.log('🧪 Test data:', testExchangeData);

        try {
            const result = await BookyAPI.createExchange(testExchangeData);
            console.log('✅ Test exchange created:', result);
            showToast('Test exchange created successfully!', 'success');
        } catch (error) {
            console.error('❌ Test exchange failed:', error);
            showToast('Test exchange failed: ' + error.message, 'error');
        }
    }

    // DEBUG: Function to inspect current wizard state
    static debugWizardState() {
        console.log('🔍 Current wizard state:');
        console.log('  exchangeData:', this.exchangeData);
        console.log('  currentUser:', currentUser);
        console.log('  authToken:', authToken);

        if (this.exchangeData) {
            console.log('  📚 Selected books:', this.exchangeData.selectedBooks);
            console.log('  👥 Selected users:', this.exchangeData.selectedUsers);
            console.log('  📖 My books to offer:', this.exchangeData.myBooksToOffer);
            console.log('  📊 Step:', this.exchangeData.step);
        } else {
            console.log('  ❌ exchangeData is null/undefined');
        }

        return this.exchangeData;
    }
}

// ===== PROFILE MANAGEMENT =====
class ProfileUI {
    static async loadProfile() {
        try {
            const profile = currentUser;
            const books = await BookyAPI.getUserBooks(currentUser.id);
            const communities = await BookyAPI.getUserCommunities(currentUser.id);
            const exchanges = await BookyAPI.getUserExchanges(currentUser.id);

            this.renderProfile(profile, books, communities, exchanges);
        } catch (error) {
            console.error('Error loading profile:', error);
        }
    }

    static renderProfile(user, books, communities, exchanges) {
        // Update profile details
        const profileDetails = document.getElementById('profileDetails');
        const userName = user?.name || 'Usuario';
        const userEmail = user?.email || 'email@ejemplo.com';
        const username = user?.username || 'usuario';

        profileDetails.innerHTML = `
            <h3>${userName}</h3>
            <p>📧 ${userEmail}</p>
            <p>👤 @${username}</p>
            ${user?.description ? `<p>📝 ${user.description}</p>` : ''}
            <p>📅 Miembro desde ${formatDate(user?.date_created || new Date())}</p>
        `;

        // Update stats with safe checks
        const booksArray = Array.isArray(books) ? books : [];
        const communitiesArray = Array.isArray(communities) ? communities : [];
        const exchangesArray = Array.isArray(exchanges) ? exchanges : [];

        document.getElementById('totalBooksCount').textContent = booksArray.length;
        document.getElementById('booksReadCount').textContent = booksArray.filter(b => b?.status === 'READ').length;
        document.getElementById('communitiesJoinedCount').textContent = communitiesArray.length;
        document.getElementById('exchangesCount').textContent = exchangesArray.length;
    }
}

// ===== EVENT LISTENERS Y INITIALIZATION =====
document.addEventListener('DOMContentLoaded', function () {
    // Auto-login with real backend authentication
    console.log('🔐 Attempting auto-login with backend...');

    // Try to login with test user credentials
    BookyAPI.login('juan.perez@gmail.com', 'password123')
        .then(response => {
            console.log('✅ Login successful:', response);
            console.log('🔑 Token received:', response.token ? `${response.token.substring(0, 50)}...` : 'NO TOKEN');
            console.log('👤 User received:', response.user);

            authToken = response.token;
            currentUser = response.user;

            // Store in localStorage
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('currentUser', JSON.stringify(currentUser));

            // Show dashboard
            document.getElementById('loginSection').style.display = 'none';
            document.getElementById('dashboard').style.display = 'block';
            document.getElementById('userInfo').style.display = 'flex';

            showToast('¡Login exitoso! Bienvenido ' + currentUser.name, 'success');
        })
        .catch(error => {
            console.error('❌ Auto-login failed:', error);
            showToast('Error en login automático: ' + error.message, 'error');
            // Show login form
            document.getElementById('loginSection').style.display = 'block';
            document.getElementById('dashboard').style.display = 'none';
        });

    // Try to restore session from localStorage if available
    if (loadFromLocalStorage()) {
        // Show dashboard
        document.getElementById('loginSection').style.display = 'none';
        document.getElementById('dashboard').style.display = 'block';
        document.getElementById('userInfo').style.display = 'flex';

        // Load initial data
        try {
            Promise.all([
                GamificationUI.updateGamificationPanel(currentUser.id),
                BooksUI.loadBooks()
            ]).then(() => {
                showToast(`¡Bienvenido de vuelta, ${currentUser.name}!`, 'success');
            }).catch(error => {
                console.error('Error loading initial data:', error);
                // If token is invalid, clear session
                clearLocalStorage();
                location.reload();
            });

            // Load users separately to avoid blocking if it fails
            try {
                UsersUI.loadUsers();
            } catch (userError) {
                console.warn('Failed to load users:', userError);
            }
        } catch (error) {
            console.error('Error restoring session:', error);
            clearLocalStorage();
        }
    }
    // Login form
    document.getElementById('loginForm').addEventListener('submit', async function (e) {
        e.preventDefault();

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await BookyAPI.login(email, password);
            authToken = response.token;
            currentUser = response.user;

            // Save session to localStorage
            saveToLocalStorage();

            // Show dashboard
            document.getElementById('loginSection').style.display = 'none';
            document.getElementById('dashboard').style.display = 'block';
            document.getElementById('userInfo').style.display = 'flex';

            // Load initial data
            try {
                await Promise.all([
                    GamificationUI.updateGamificationPanel(currentUser.id),
                    BooksUI.loadBooks()
                ]);

                // Load users separately to avoid blocking if it fails
                try {
                    await UsersUI.loadUsers();
                } catch (userError) {
                    console.warn('Failed to load users:', userError);
                }
            } catch (error) {
                console.error('Error loading initial data:', error);
            }

            showToast(`¡Bienvenido, ${currentUser.name}!`, 'success');
        } catch (error) {
            showToast('Error en el login. Verifica tus credenciales.', 'error');
        }
    });

    // Logout
    document.getElementById('logoutBtn').addEventListener('click', function () {
        authToken = null;
        currentUser = null;
        followingUsers = [];
        followers = [];

        // Clear localStorage
        clearLocalStorage();

        document.getElementById('loginSection').style.display = 'block';
        document.getElementById('dashboard').style.display = 'none';
        document.getElementById('userInfo').style.display = 'none';

        showToast('Sesión cerrada', 'info');
    });

    // Tab navigation
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const tabName = this.dataset.tab;

            // Update active tab
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            this.classList.add('active');
            document.getElementById(tabName + 'Tab').classList.add('active');

            // Load tab content
            switch (tabName) {
                case 'books':
                    BooksUI.loadBooks();
                    break;
                case 'communities':
                    CommunitiesUI.loadCommunities();
                    break;
                case 'readingClubs':
                    ReadingClubsUI.loadReadingClubs();
                    break;
                case 'posts':
                    PostsUI.loadPosts();
                    break;
                case 'exchanges':
                    ExchangesUI.loadExchanges();
                    break;
                case 'users':
                    UsersUI.loadUsers();
                    break;
                case 'profile':
                    ProfileUI.loadProfile();
                    break;
            }
        });
    });

    // Form submissions
    document.getElementById('addBookByIsbnForm').addEventListener('submit', async function (e) {
        e.preventDefault();
        const formData = new FormData();
        formData.append('isbn', document.getElementById('bookISBNSearch').value);
        formData.append('status', document.getElementById('bookStatusIsbn').value);
        await BooksUI.addBookByIsbn(formData);
        this.reset();
        document.getElementById('bookPreview').style.display = 'none';
        document.getElementById('addByIsbnBtn').disabled = true;
    });

    document.getElementById('createCommunityForm').addEventListener('submit', async function (e) {
        e.preventDefault();
        await CommunitiesUI.createCommunity(new FormData(this));
        this.reset();
    });

    document.getElementById('createReadingClubForm').addEventListener('submit', async function (e) {
        e.preventDefault();
        await ReadingClubsUI.createReadingClub(new FormData(this));
        this.reset();
    });

    document.getElementById('createPostForm').addEventListener('submit', async function (e) {
        e.preventDefault();
        await PostsUI.createPost(new FormData(this));
        this.reset();
    });

    // Modal controls
    document.getElementById('addBookBtn').addEventListener('click', function () {
        document.getElementById('addBookModal').style.display = 'block';
    });

    document.getElementById('createCommunityBtn').addEventListener('click', async function () {
        // Load communities for dropdown
        await loadCommunitiesForDropdown();
        document.getElementById('createCommunityModal').style.display = 'block';
    });

    document.getElementById('createReadingClubBtn').addEventListener('click', async function () {
        // Load communities and books for dropdowns
        await loadCommunitiesForDropdown();
        await loadBooksForDropdown();
        document.getElementById('createReadingClubModal').style.display = 'block';
    });

    document.getElementById('createPostBtn').addEventListener('click', async function () {
        // Load communities for dropdown
        await loadCommunitiesForDropdown();
        document.getElementById('createPostModal').style.display = 'block';
    });

    // Filters
    document.getElementById('statusFilter').addEventListener('change', BooksUI.filterBooks);
    document.getElementById('exchangeFilter').addEventListener('change', BooksUI.filterBooks);
    document.getElementById('communityPostFilter').addEventListener('change', PostsUI.loadPosts);

    // Search
    document.getElementById('searchUsersBtn').addEventListener('click', UsersUI.searchUsers);
    document.getElementById('clearSearchBtn').addEventListener('click', UsersUI.clearSearch);
    document.getElementById('userSearchInput').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            UsersUI.searchUsers();
        }
    });

    // Auto-search after typing (debounced)
    let searchTimeout;
    document.getElementById('userSearchInput').addEventListener('input', function (e) {
        clearTimeout(searchTimeout);
        const query = e.target.value.trim();

        if (query.length === 0) {
            // Clear search when input is empty
            UsersUI.clearSearch();
        } else if (query.length >= 3) {
            // Auto-search after 3 characters with 500ms delay
            searchTimeout = setTimeout(() => {
                UsersUI.searchUsers();
            }, 500);
        }
    });

    // Community/Club buttons
    document.getElementById('myCommunitiesBtn').addEventListener('click', CommunitiesUI.loadMyCommunities);
    document.getElementById('myClubsBtn').addEventListener('click', ReadingClubsUI.loadMyClubs);

    // Exchange button
    document.getElementById('createExchangeBtn').addEventListener('click', () => {
        ExchangesUI.startExchangeWizard();
    });

    // ISBN search button
    document.getElementById('searchByIsbnBtn').addEventListener('click', BooksUI.searchBookByIsbn);

    // Close modals
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', function () {
            this.closest('.modal').style.display = 'none';
        });
    });

    // Close modal when clicking outside
    window.addEventListener('click', function (e) {
        if (e.target.classList.contains('modal')) {
            e.target.style.display = 'none';
        }
    });
});

// ===== HELPER FUNCTIONS =====
async function loadCommunitiesForDropdown() {
    try {
        const communities = await BookyAPI.getCommunities();

        // Update all community dropdowns
        const dropdowns = ['clubCommunity', 'postCommunity', 'communityPostFilter'];
        dropdowns.forEach(id => {
            const select = document.getElementById(id);
            if (select) {
                const currentValue = select.value;
                select.innerHTML = id === 'communityPostFilter' ? '<option value="">Todas las comunidades</option>' : '<option value="">Selecciona una comunidad</option>';

                communities.forEach(community => {
                    const option = document.createElement('option');
                    option.value = community.id;
                    option.textContent = community.name;
                    select.appendChild(option);
                });

                select.value = currentValue;
            }
        });
    } catch (error) {
        console.error('Error loading communities for dropdown:', error);
    }
}

async function loadBooksForDropdown() {
    try {
        const books = await BookyAPI.getUserBooks(currentUser.id);
        const select = document.getElementById('clubBook');

        select.innerHTML = '<option value="">Selecciona un libro</option>';
        books.forEach(book => {
            const option = document.createElement('option');
            option.value = book.book.id;
            option.textContent = `${book.book.title} - ${book.book.author}`;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading books for dropdown:', error);
    }
}

// ===== AUTO-REFRESH =====
setInterval(async function () {
    if (currentUser && authToken) {
        try {
            await GamificationUI.updateGamificationPanel(currentUser.id);
        } catch (error) {
            console.log('Auto-refresh failed:', error);
        }
    }
}, 30000); // Refresh every 30 seconds