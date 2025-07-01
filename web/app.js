// Configuration
const API_BASE = 'http://localhost:8080';
let currentUser = null;
let authToken = null;

// DOM Elements
const loginSection = document.getElementById('login-section');
const app = document.getElementById('app');
const currentUserSpan = document.getElementById('current-user');
const logoutBtn = document.getElementById('logout-btn');
const userSelect = document.getElementById('user-select');
const loginBtn = document.getElementById('login-btn');
const loadingDiv = document.getElementById('loading');
const errorDiv = document.getElementById('error');

// User mapping from username to email
function getEmailByUsername(username) {
    const userMap = {
        'admin': 'admin@booky.com',
        'juanp': 'juan.perez@gmail.com',
        'mariag': 'maria.garcia@outlook.com',
        'carlosr': 'carlos.rodriguez@yahoo.com',
        'anal': 'ana.lopez@gmail.com',
        'luism': 'luis.martinez@hotmail.com',
        'sofiag': 'sofia.gonzalez@gmail.com',
        'diegof': 'diego.fernandez@yahoo.com',
        'luciam': 'lucia.morales@outlook.com',
        'alejandros': 'alejandro.silva@gmail.com',
        'valentinac': 'valentina.castro@hotmail.com'
    };
    return userMap[username] || username;
}

// Initialize
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
    initializeTabs();
});

// Event Listeners
function initializeEventListeners() {
    // Login
    loginBtn.addEventListener('click', handleLogin);
    logoutBtn.addEventListener('click', handleLogout);

    // Posts
    document.getElementById('load-feed').addEventListener('click', loadUserFeed);
    document.getElementById('load-all-posts').addEventListener('click', loadAllPosts);
    document.getElementById('show-create-post').addEventListener('click', showCreatePostForm);
    document.getElementById('create-post-btn').addEventListener('click', createPost);
    document.getElementById('cancel-post-btn').addEventListener('click', hideCreatePostForm);

    // Communities
    document.getElementById('load-communities').addEventListener('click', loadAllCommunities);
    document.getElementById('load-my-communities').addEventListener('click', loadMyCommunities);
    document.getElementById('show-create-community').addEventListener('click', showCreateCommunityForm);
    document.getElementById('create-community-btn').addEventListener('click', createCommunity);
    document.getElementById('cancel-community-btn').addEventListener('click', hideCreateCommunityForm);

    // Clubs
    document.getElementById('load-clubs').addEventListener('click', loadAllClubs);
    document.getElementById('load-my-clubs').addEventListener('click', loadMyClubs);

    // Users
    document.getElementById('load-users').addEventListener('click', loadAllUsers);

    // Books
    document.getElementById('search-books-btn').addEventListener('click', searchBooks);
    document.getElementById('book-search').addEventListener('keypress', function (event) {
        if (event.key === 'Enter') {
            searchBooks();
        }
    });
}

// Tab Navigation
function initializeTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;

            // Update tab buttons
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // Update tab content
            tabContents.forEach(content => {
                content.classList.remove('active');
                if (content.id === `${tabName}-tab`) {
                    content.classList.add('active');
                }
            });
        });
    });
}

// Authentication
async function handleLogin() {
    const username = userSelect.value;
    if (!username) {
        showError('Por favor selecciona un usuario');
        return;
    }

    showLoading();

    try {
        const response = await fetch(`${API_BASE}/sign-in`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                email: getEmailByUsername(username),
                password: 'password123'
            })
        });

        if (response.ok) {
            const userData = await response.json();
            authToken = 'simulated-token';
            currentUser = {
                id: userData.id,
                username: userData.username,
                name: userData.name,
                lastname: userData.lastname,
                email: userData.email
            };

            // Update UI
            currentUserSpan.textContent = `${currentUser.name} ${currentUser.lastname} (@${currentUser.username})`;
            loginSection.style.display = 'none';
            app.style.display = 'block';
            logoutBtn.style.display = 'inline-block';

            // Load initial data
            await loadCommunityOptions();
            loadAllPosts();

            showSuccess('¡Login exitoso!');
        } else {
            showError('Error en el login: ' + response.statusText);
        }
    } catch (error) {
        showError('Error conectando con el servidor: ' + error.message);
    } finally {
        hideLoading();
    }
}

function handleLogout() {
    authToken = null;
    currentUser = null;
    currentUserSpan.textContent = 'No logueado';
    loginSection.style.display = 'block';
    app.style.display = 'none';
    logoutBtn.style.display = 'none';
    clearAllContent();
}

// API Helper
async function apiRequest(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json'
        }
    };

    const response = await fetch(`${API_BASE}${url}`, {
        ...defaultOptions,
        ...options,
        headers: { ...defaultOptions.headers, ...options.headers }
    });

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
}

// Posts Functions
async function loadUserFeed() {
    showLoading();
    try {
        const posts = await apiRequest('/posts/feed');
        displayPosts(posts, '🏠 Tu Feed Personal');
    } catch (error) {
        showError('Error cargando feed: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function loadAllPosts() {
    showLoading();
    try {
        const posts = await apiRequest('/posts');
        displayPosts(posts, '🌍 Todos los Posts');
    } catch (error) {
        showError('Error cargando posts: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function createPost() {
    const body = document.getElementById('post-body').value.trim();
    const communityId = document.getElementById('post-community').value || null;

    if (!body) {
        showError('El contenido del post no puede estar vacío');
        return;
    }

    showLoading();
    try {
        const postData = {
            body: body,
            communityId: communityId
        };

        await apiRequest('/posts', {
            method: 'POST',
            body: JSON.stringify(postData)
        });

        // Clear form
        document.getElementById('post-body').value = '';
        document.getElementById('post-community').value = '';
        hideCreatePostForm();

        // Reload posts
        loadAllPosts();
        showSuccess('¡Post creado exitosamente!');
    } catch (error) {
        showError('Error creando post: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayPosts(posts, title) {
    const postsContainer = document.getElementById('posts-list');

    if (!posts || posts.length === 0) {
        postsContainer.innerHTML = `
            <div class="post-card">
                <h3>${title}</h3>
                <p>No hay posts disponibles.</p>
            </div>
        `;
        return;
    }

    postsContainer.innerHTML = `<h3>${title}</h3>` + posts.map(post => `
        <div class="post-card">
            <div class="post-header">
                <div class="author-info">
                    <div class="author-avatar">
                        ${post.user.name.charAt(0)}${post.user.lastname.charAt(0)}
                    </div>
                    <div>
                        <div class="author-name">${post.user.name} ${post.user.lastname}</div>
                        <div class="post-time">${formatDate(post.dateCreated)}</div>
                    </div>
                </div>
                ${post.community ? `<span class="community-badge">${post.community.name}</span>` : ''}
            </div>
            <div class="post-body">${post.body}</div>
            <div class="post-actions">
                <button onclick="loadPostComments('${post.id}')">💬 Ver Comentarios</button>
                ${post.user.id === currentUser.id ? `<button onclick="deletePost('${post.id}')">🗑️ Eliminar</button>` : ''}
            </div>
            <div id="comments-${post.id}" class="comments-section" style="display: none;"></div>
        </div>
    `).join('');
}

// Communities Functions
async function loadAllCommunities() {
    showLoading();
    try {
        const communities = await apiRequest('/communities');
        displayCommunities(communities, '🏘️ Todas las Comunidades');
    } catch (error) {
        showError('Error cargando comunidades: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function showCreateCommunityForm() {
    document.getElementById('create-community-form').style.display = 'block';
}

function hideCreateCommunityForm() {
    document.getElementById('create-community-form').style.display = 'none';
}

async function createCommunity() {
    const name = document.getElementById('community-name').value.trim();
    const description = document.getElementById('community-description').value.trim();

    if (!name || !description) {
        showError('El nombre y la descripción son obligatorios');
        return;
    }

    showLoading();
    try {
        const communityData = {
            name: name,
            description: description
        };

        await apiRequest('/communities', {
            method: 'POST',
            body: JSON.stringify(communityData)
        });

        // Clear form
        document.getElementById('community-name').value = '';
        document.getElementById('community-description').value = '';
        hideCreateCommunityForm();

        // Reload communities
        loadAllCommunities();
        showSuccess('¡Comunidad creada exitosamente!');
    } catch (error) {
        showError('Error creando comunidad: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function loadMyCommunities() {
    showLoading();
    try {
        const communities = await apiRequest(`/communities/user/${currentUser.id}`);
        displayCommunities(communities, '👤 Mis Comunidades');
    } catch (error) {
        showError('Error cargando mis comunidades: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayCommunities(communities, title) {
    const container = document.getElementById('communities-list');

    if (!communities || communities.length === 0) {
        container.innerHTML = `
            <div class="community-card">
                <h3>${title}</h3>
                <p>No hay comunidades disponibles.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `<h3>${title}</h3>` + communities.map(community => `
        <div class="community-card">
            <div class="card-header">
                <h4>${community.name}</h4>
                <div class="card-meta">Admin: ${community.admin.name}</div>
            </div>
            <div class="card-description">${community.description}</div>
            <div class="stats">
                <div class="stat">
                    <span>📅</span>
                    <span>Creada: ${formatDate(community.dateCreated)}</span>
                </div>
            </div>
            <div class="card-actions">
                <button onclick="loadCommunityPosts('${community.id}')">📝 Ver Posts</button>
                <button onclick="joinCommunity('${community.id}')">➕ Unirse</button>
            </div>
        </div>
    `).join('');
}

// Reading Clubs Functions
async function loadAllClubs() {
    showLoading();
    try {
        const clubs = await apiRequest('/reading-clubs');
        displayClubs(clubs, '📖 Todos los Clubes de Lectura');
    } catch (error) {
        showError('Error cargando clubes: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function loadMyClubs() {
    showLoading();
    try {
        const clubs = await apiRequest(`/reading-clubs/user/${currentUser.id}`);
        displayClubs(clubs, '👤 Mis Clubes de Lectura');
    } catch (error) {
        showError('Error cargando mis clubes: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayClubs(clubs, title) {
    const container = document.getElementById('clubs-list');

    if (!clubs || clubs.length === 0) {
        container.innerHTML = `
            <div class="club-card">
                <h3>${title}</h3>
                <p>No hay clubes disponibles.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `<h3>${title}</h3>` + clubs.map(club => `
        <div class="club-card">
            <div class="card-header">
                <h4>${club.name}</h4>
                <div class="card-meta">Moderador: ${club.moderator.name}</div>
            </div>
            <div class="card-description">${club.description}</div>
            <div class="stats">
                <div class="stat">
                    <span>📚</span>
                    <span>Libro: ${club.book.title}</span>
                </div>
                <div class="stat">
                    <span>✍️</span>
                    <span>Autor: ${club.book.author}</span>
                </div>
                <div class="stat">
                    <span>🏘️</span>
                    <span>Comunidad: ${club.community.name}</span>
                </div>
            </div>
            <div class="card-actions">
                <button onclick="joinClub('${club.id}')">➕ Unirse al Club</button>
                <button onclick="viewBookDetails('${club.book.id}')">📖 Ver Libro</button>
            </div>
        </div>
    `).join('');
}

// Users Functions
async function loadAllUsers() {
    showLoading();
    try {
        const users = await apiRequest('/users');
        displayUsers(users, '👥 Todos los Usuarios');
    } catch (error) {
        showError('Error cargando usuarios: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayUsers(users, title) {
    const container = document.getElementById('users-list');

    if (!users || users.length === 0) {
        container.innerHTML = `
            <div class="user-card">
                <h3>${title}</h3>
                <p>No hay usuarios disponibles.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `<h3>${title}</h3>` + users.map(user => `
        <div class="user-card">
            <div class="card-header">
                <div class="author-info">
                    <div class="author-avatar">
                        ${user.name.charAt(0)}${user.lastname.charAt(0)}
                    </div>
                    <div>
                        <div class="author-name">${user.name} ${user.lastname}</div>
                        <div class="card-meta">@${user.username}</div>
                    </div>
                </div>
                <div class="stat">
                    <span class="stat-number">${user.coins}</span>
                    <span>🪙 coins</span>
                </div>
            </div>
            <div class="card-description">${user.description || 'Sin descripción'}</div>
            <div class="stats">
                <div class="stat">
                    <span>📍</span>
                    <span>${user.address ? `${user.address.state}, ${user.address.country}` : 'Sin ubicación'}</span>
                </div>
                <div class="stat">
                    <span>📅</span>
                    <span>Miembro desde: ${formatDate(user.dateCreated)}</span>
                </div>
            </div>
            <div class="card-actions">
                ${user.id !== currentUser.id ? `<button onclick="followUser('${user.id}')">➕ Seguir</button>` : ''}
                <button onclick="loadUserPosts('${user.id}')">📝 Ver Posts</button>
            </div>
        </div>
    `).join('');
}

// Helper Functions
async function loadCommunityOptions() {
    try {
        const communities = await apiRequest('/communities');
        const select = document.getElementById('post-community');
        select.innerHTML = '<option value="">Post General (sin comunidad)</option>';
        communities.forEach(community => {
            select.innerHTML += `<option value="${community.id}">${community.name}</option>`;
        });
    } catch (error) {
        console.error('Error loading communities for select:', error);
    }
}

async function loadPostComments(postId) {
    const commentsContainer = document.getElementById(`comments-${postId}`);

    if (commentsContainer.style.display === 'block') {
        commentsContainer.style.display = 'none';
        return;
    }

    try {
        const comments = await apiRequest(`/posts/${postId}/comments`);

        if (comments && comments.length > 0) {
            commentsContainer.innerHTML = comments.map(comment => `
                <div class="comment">
                    <div class="comment-author">${comment.user.name} ${comment.user.lastname}</div>
                    <div class="comment-body">${comment.body}</div>
                </div>
            `).join('');
        } else {
            commentsContainer.innerHTML = '<p>No hay comentarios aún.</p>';
        }

        commentsContainer.style.display = 'block';
    } catch (error) {
        commentsContainer.innerHTML = '<p>Error cargando comentarios.</p>';
        commentsContainer.style.display = 'block';
    }
}

function showCreatePostForm() {
    document.getElementById('create-post-form').style.display = 'block';
}

function hideCreatePostForm() {
    document.getElementById('create-post-form').style.display = 'none';
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) return 'Hace 1 día';
    if (diffDays < 7) return `Hace ${diffDays} días`;
    if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semanas`;

    return date.toLocaleDateString('es-ES');
}

function showLoading() {
    loadingDiv.style.display = 'block';
    errorDiv.style.display = 'none';
}

function hideLoading() {
    loadingDiv.style.display = 'none';
}

function showError(message) {
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000);
}

function showSuccess(message) {
    const successDiv = document.createElement('div');
    successDiv.className = 'success';
    successDiv.textContent = message;
    document.querySelector('.container').insertBefore(successDiv, document.querySelector('.container').firstChild.nextSibling);

    setTimeout(() => {
        successDiv.remove();
    }, 3000);
}

function clearAllContent() {
    document.getElementById('posts-list').innerHTML = '';
    document.getElementById('communities-list').innerHTML = '';
    document.getElementById('clubs-list').innerHTML = '';
    document.getElementById('books-list').innerHTML = '';
    document.getElementById('users-list').innerHTML = '';
}

// Placeholder functions for actions
async function deletePost(postId) {
    if (confirm('¿Estás seguro de que quieres eliminar este post?')) {
        try {
            await apiRequest(`/posts/${postId}`, { method: 'DELETE' });
            loadAllPosts();
            showSuccess('Post eliminado exitosamente');
        } catch (error) {
            showError('Error eliminando post: ' + error.message);
        }
    }
}

function joinCommunity(communityId) {
    showSuccess('Función de unirse a comunidad no implementada aún');
}

function joinClub(clubId) {
    showSuccess('Función de unirse a club no implementada aún');
}

function followUser(userId) {
    showSuccess('Función de seguir usuario no implementada aún');
}

async function loadCommunityPosts(communityId) {
    showLoading();
    try {
        const posts = await apiRequest(`/posts/community/${communityId}`);
        displayPosts(posts, `📋 Posts de Comunidad`);
    } catch (error) {
        showError('Error cargando posts de comunidad: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function joinCommunity(communityId) {
    showLoading();
    try {
        await apiRequest(`/communities/${communityId}/join`, {
            method: 'POST'
        });
        showSuccess('¡Te has unido a la comunidad exitosamente!');
        loadAllCommunities(); // Refresh the communities list
    } catch (error) {
        showError('Error uniéndose a la comunidad: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function loadPostComments(postId) {
    const container = document.getElementById(`comments-${postId}`);

    if (container.style.display === 'block') {
        container.style.display = 'none';
        return;
    }

    showLoading();
    try {
        const comments = await apiRequest(`/comments/post/${postId}`);
        displayComments(comments, postId);
        container.style.display = 'block';
    } catch (error) {
        showError('Error cargando comentarios: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayComments(comments, postId) {
    const container = document.getElementById(`comments-${postId}`);

    if (!comments || comments.length === 0) {
        container.innerHTML = `
            <div class="comments-header">
                <h4>💬 Comentarios</h4>
                <button onclick="showAddCommentForm('${postId}')">➕ Agregar Comentario</button>
            </div>
            <div id="add-comment-${postId}" class="add-comment-form" style="display: none;">
                <textarea id="comment-body-${postId}" placeholder="Escribe tu comentario..."></textarea>
                <div class="actions">
                    <button onclick="addComment('${postId}')">Comentar</button>
                    <button onclick="hideAddCommentForm('${postId}')">Cancelar</button>
                </div>
            </div>
            <p>No hay comentarios aún.</p>
        `;
        return;
    }

    container.innerHTML = `
        <div class="comments-header">
            <h4>💬 Comentarios (${comments.length})</h4>
            <button onclick="showAddCommentForm('${postId}')">➕ Agregar Comentario</button>
        </div>
        <div id="add-comment-${postId}" class="add-comment-form" style="display: none;">
            <textarea id="comment-body-${postId}" placeholder="Escribe tu comentario..."></textarea>
            <div class="actions">
                <button onclick="addComment('${postId}')">Comentar</button>
                <button onclick="hideAddCommentForm('${postId}')">Cancelar</button>
            </div>
        </div>
        ${comments.map(comment => `
            <div class="comment">
                <div class="comment-header">
                    <div class="comment-author">
                        <div class="author-avatar">
                            ${comment.user.name.charAt(0)}${comment.user.lastname.charAt(0)}
                        </div>
                        <div>
                            <div class="author-name">${comment.user.name} ${comment.user.lastname}</div>
                            <div class="comment-time">${formatDate(comment.dateCreated)}</div>
                        </div>
                    </div>
                </div>
                <div class="comment-body">${comment.body}</div>
            </div>
        `).join('')}
    `;
}

function showAddCommentForm(postId) {
    document.getElementById(`add-comment-${postId}`).style.display = 'block';
}

function hideAddCommentForm(postId) {
    document.getElementById(`add-comment-${postId}`).style.display = 'none';
    document.getElementById(`comment-body-${postId}`).value = '';
}

async function addComment(postId) {
    const body = document.getElementById(`comment-body-${postId}`).value.trim();

    if (!body) {
        showError('El comentario no puede estar vacío');
        return;
    }

    showLoading();
    try {
        const commentData = {
            body: body,
            postId: postId
        };

        await apiRequest('/comments', {
            method: 'POST',
            body: JSON.stringify(commentData)
        });

        hideAddCommentForm(postId);
        // Reload comments
        document.getElementById(`comments-${postId}`).style.display = 'none';
        loadPostComments(postId);
        showSuccess('¡Comentario agregado exitosamente!');
    } catch (error) {
        showError('Error agregando comentario: ' + error.message);
    } finally {
        hideLoading();
    }
}

function loadUserPosts(userId) {
    showSuccess(`Cargando posts de usuario ${userId} - No implementado aún`);
}

function viewBookDetails(bookId) {
    showSuccess(`Ver detalles del libro ${bookId} - No implementado aún`);
}

// Books Functions
async function searchBooks() {
    const query = document.getElementById('book-search').value.trim();

    if (!query) {
        showError('Por favor ingresa un término de búsqueda');
        return;
    }

    showLoading();
    try {
        const books = await apiRequest(`/books/search?q=${encodeURIComponent(query)}`);
        displayBooks(books, `📚 Resultados para: "${query}"`);
    } catch (error) {
        showError('Error buscando libros: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displayBooks(books, title) {
    const container = document.getElementById('books-list');

    if (books.length === 0) {
        container.innerHTML = `
            <div class="section">
                <h2>${title}</h2>
                <p class="no-content">No se encontraron libros con ese término de búsqueda.</p>
                <p class="help-text">💡 Intenta buscar por título, autor o categoría</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="section">
            <h2>${title}</h2>
            <div class="books-grid">
                ${books.map(book => `
                    <div class="book-card">
                        <div class="book-image">
                            ${book.image ?
            `<img src="${book.image}" alt="${book.title}" onerror="this.style.display='none'">` :
            '<div class="book-placeholder">📚</div>'
        }
                        </div>
                        <div class="book-info">
                            <h3 class="book-title">${book.title || 'Sin título'}</h3>
                            <p class="book-author">👤 ${book.author || 'Autor desconocido'}</p>
                            ${book.isbn ? `<p class="book-isbn">📖 ISBN: ${book.isbn}</p>` : ''}
                            ${book.publisher ? `<p class="book-publisher">🏢 ${book.publisher}</p>` : ''}
                            ${book.pages ? `<p class="book-pages">📄 ${book.pages} páginas</p>` : ''}
                            ${book.categories && book.categories.length > 0 ?
            `<div class="book-categories">
                                    🏷️ ${book.categories.map(cat => `<span class="category-tag">${cat}</span>`).join(' ')}
                                </div>` : ''
        }
                            ${book.overview ? `<p class="book-overview">${book.overview}</p>` : ''}
                        </div>
                        <div class="book-actions">
                            <button onclick="addBookToLibrary('${book.isbn}')" class="btn-primary">
                                ➕ Agregar a Mi Biblioteca
                            </button>
                            ${book.isbn ?
            `<button onclick="viewBookByIsbn('${book.isbn}')" class="btn-secondary">
                                    👁️ Ver Detalles
                                </button>` : ''
        }
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>
    `;
}

async function addBookToLibrary(isbn) {
    if (!currentUser) {
        showError('Debes estar logueado para agregar libros');
        return;
    }

    try {
        showLoading();
        await apiRequest(`/books/users/${currentUser.id}/library`, {
            method: 'POST',
            body: JSON.stringify({
                isbn: isbn,
                status: 'WANT_TO_READ'
            })
        });
        showSuccess('📚 ¡Libro agregado a tu biblioteca!');
    } catch (error) {
        if (error.message.includes('409')) {
            showError('Este libro ya está en tu biblioteca');
        } else {
            showError('Error agregando libro: ' + error.message);
        }
    } finally {
        hideLoading();
    }
}

async function viewBookByIsbn(isbn) {
    try {
        showLoading();
        const book = await apiRequest(`/books/isbn/${isbn}`);

        // Crear modal o mostrar detalles
        alert(`📚 ${book.title}\n👤 ${book.author}\n📖 ${book.isbn}\n\n${book.synopsis || book.overview || 'Sin descripción disponible'}`);
    } catch (error) {
        showError('Error cargando detalles del libro: ' + error.message);
    } finally {
        hideLoading();
    }
} 