// Demo version with mock data - for testing without backend
const DEMO_MODE = true;

// Mock Data
const mockUsers = [
    { id: 'user-001', username: 'juanp', name: 'Juan', lastname: 'Pérez', description: 'Amante de la literatura clásica', coins: 150, address: { state: 'Buenos Aires', country: 'Argentina' }, dateCreated: '2024-01-15T10:00:00Z' },
    { id: 'user-002', username: 'mariag', name: 'María', lastname: 'García', description: 'Lectora de misterio', coins: 200, address: { state: 'Córdoba', country: 'Argentina' }, dateCreated: '2024-01-16T10:00:00Z' },
    { id: 'user-003', username: 'carlosr', name: 'Carlos', lastname: 'Rodríguez', description: 'Fanático de sci-fi', coins: 175, address: { state: 'Rosario', country: 'Argentina' }, dateCreated: '2024-01-17T10:00:00Z' },
    { id: 'admin-001', username: 'admin', name: 'Administrador', lastname: 'Sistema', description: 'Admin del sistema', coins: 1000, address: { state: 'Buenos Aires', country: 'Argentina' }, dateCreated: '2024-01-01T10:00:00Z' }
];

const mockPosts = [
    {
        id: 'post-001',
        body: '¡Acabo de terminar "Cien años de soledad"! La magia de García Márquez es incomparable. ¿Alguien más se quedó viviendo en Macondo? 📚✨',
        dateCreated: '2024-06-30T20:00:00Z',
        user: mockUsers[0],
        community: null
    },
    {
        id: 'post-002',
        body: 'Organizando mi biblioteca y tengo más libros sin leer que leídos. ¿Les pasa lo mismo? #ProblemasDeUnaLectora',
        dateCreated: '2024-06-30T17:00:00Z',
        user: mockUsers[1],
        community: null
    },
    {
        id: 'post-003',
        body: '¿Es Emma Bovary víctima del romanticismo o adelantada a su tiempo? Flaubert creó un personaje complejo. Abramos el debate.',
        dateCreated: '2024-06-30T15:00:00Z',
        user: mockUsers[0],
        community: { id: 'comm-001', name: 'Literatura Clásica' }
    }
];

const mockCommunities = [
    { id: 'comm-001', name: 'Literatura Clásica', description: 'Espacio para amantes de la literatura clásica y contemporánea', admin: mockUsers[0], dateCreated: '2024-01-20T10:00:00Z' },
    { id: 'comm-002', name: 'Club de Misterio', description: 'Comunidad dedicada a los misterios más intrigantes de la literatura', admin: mockUsers[1], dateCreated: '2024-01-21T10:00:00Z' },
    { id: 'comm-003', name: 'Sci-Fi & Fantasy', description: 'Para fanáticos de mundos fantásticos y ciencia ficción', admin: mockUsers[2], dateCreated: '2024-01-22T10:00:00Z' }
];

const mockClubs = [
    {
        id: 'club-001',
        name: 'Club Mockingbird',
        description: 'Club dedicado a la lectura de "To Kill a Mockingbird"',
        book: { id: '1', title: 'To Kill a Mockingbird', author: 'Harper Lee' },
        community: mockCommunities[0],
        moderator: mockUsers[0],
        dateCreated: '2024-02-01T10:00:00Z'
    },
    {
        id: 'club-002',
        name: 'Hermano Mayor',
        description: 'Análisis de "1984" de Orwell en tiempos modernos',
        book: { id: '2', title: '1984', author: 'George Orwell' },
        community: mockCommunities[2],
        moderator: mockUsers[2],
        dateCreated: '2024-02-02T10:00:00Z'
    }
];

const mockComments = {
    'post-001': [
        { id: 'comm-001', body: 'Macondo es más real que muchos lugares reales. García Márquez tenía esa magia única.', user: mockUsers[1], dateCreated: '2024-06-30T21:00:00Z' },
        { id: 'comm-002', body: 'A mí me pasó lo mismo con "El amor en los tiempos del cólera". García Márquez escribía con el corazón.', user: mockUsers[2], dateCreated: '2024-06-30T21:30:00Z' }
    ],
    'post-002': [
        { id: 'comm-003', body: '¡Ese es el síndrome del TBR infinito! Parte de la magia de ser lector.', user: mockUsers[0], dateCreated: '2024-06-30T18:00:00Z' }
    ]
};

// Current user simulation
let currentUser = null;
let authToken = 'demo-token';

// DOM Elements (same as original)
const loginSection = document.getElementById('login-section');
const app = document.getElementById('app');
const currentUserSpan = document.getElementById('current-user');
const logoutBtn = document.getElementById('logout-btn');
const userSelect = document.getElementById('user-select');
const loginBtn = document.getElementById('login-btn');
const loadingDiv = document.getElementById('loading');
const errorDiv = document.getElementById('error');

// Initialize
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
    initializeTabs();
    showDemoNotice();
});

function showDemoNotice() {
    const notice = document.createElement('div');
    notice.className = 'success';
    notice.innerHTML = '🎮 MODO DEMO: Usando datos de ejemplo. El backend no está conectado.';
    document.querySelector('.container').insertBefore(notice, document.querySelector('header').nextSibling);
}

// Event Listeners (same structure as original)
function initializeEventListeners() {
    loginBtn.addEventListener('click', handleLogin);
    logoutBtn.addEventListener('click', handleLogout);

    document.getElementById('load-feed').addEventListener('click', loadUserFeed);
    document.getElementById('load-all-posts').addEventListener('click', loadAllPosts);
    document.getElementById('show-create-post').addEventListener('click', showCreatePostForm);
    document.getElementById('create-post-btn').addEventListener('click', createPost);
    document.getElementById('cancel-post-btn').addEventListener('click', hideCreatePostForm);

    document.getElementById('load-communities').addEventListener('click', loadAllCommunities);
    document.getElementById('load-my-communities').addEventListener('click', loadMyCommunities);

    document.getElementById('load-clubs').addEventListener('click', loadAllClubs);
    document.getElementById('load-my-clubs').addEventListener('click', loadMyClubs);

    document.getElementById('load-users').addEventListener('click', loadAllUsers);
}

// Tab Navigation (same as original)
function initializeTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;

            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            tabContents.forEach(content => {
                content.classList.remove('active');
                if (content.id === `${tabName}-tab`) {
                    content.classList.add('active');
                }
            });
        });
    });
}

// Authentication (Demo version)
async function handleLogin() {
    const username = userSelect.value;
    if (!username) {
        showError('Por favor selecciona un usuario');
        return;
    }

    showLoading();

    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Find user in mock data
    currentUser = mockUsers.find(user => user.username === username);
    if (!currentUser) {
        showError('Usuario no encontrado');
        hideLoading();
        return;
    }

    // Update UI
    currentUserSpan.textContent = `${currentUser.name} ${currentUser.lastname} (@${currentUser.username})`;
    loginSection.style.display = 'none';
    app.style.display = 'block';
    logoutBtn.style.display = 'inline-block';

    // Load initial data
    loadCommunityOptions();
    loadAllPosts();

    showSuccess('¡Login exitoso! (Modo Demo)');
    hideLoading();
}

function handleLogout() {
    currentUser = null;
    currentUserSpan.textContent = 'No logueado';
    loginSection.style.display = 'block';
    app.style.display = 'none';
    logoutBtn.style.display = 'none';
    clearAllContent();
}

// Posts Functions (Demo versions)
async function loadUserFeed() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Filter posts from users that current user "follows" (demo logic)
    const feedPosts = mockPosts.filter(post => post.user.id !== currentUser.id);
    displayPosts(feedPosts, '🏠 Tu Feed Personal (Demo)');
    hideLoading();
}

async function loadAllPosts() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    displayPosts(mockPosts, '🌍 Todos los Posts (Demo)');
    hideLoading();
}

async function createPost() {
    const body = document.getElementById('post-body').value.trim();
    const communityId = document.getElementById('post-community').value || null;

    if (!body) {
        showError('El contenido del post no puede estar vacío');
        return;
    }

    showLoading();
    await new Promise(resolve => setTimeout(resolve, 800));

    // Create new post (demo)
    const newPost = {
        id: `post-${Date.now()}`,
        body: body,
        dateCreated: new Date().toISOString(),
        user: currentUser,
        community: communityId ? mockCommunities.find(c => c.id === communityId) : null
    };

    mockPosts.unshift(newPost);

    // Clear form
    document.getElementById('post-body').value = '';
    document.getElementById('post-community').value = '';
    hideCreatePostForm();

    loadAllPosts();
    showSuccess('¡Post creado exitosamente! (Demo)');
    hideLoading();
}

// Communities Functions (Demo versions)
async function loadAllCommunities() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    displayCommunities(mockCommunities, '🏘️ Todas las Comunidades (Demo)');
    hideLoading();
}

async function loadMyCommunities() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    // Demo: user is member of first 2 communities
    const myCommunities = mockCommunities.slice(0, 2);
    displayCommunities(myCommunities, '👤 Mis Comunidades (Demo)');
    hideLoading();
}

// Clubs Functions (Demo versions)
async function loadAllClubs() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    displayClubs(mockClubs, '📖 Todos los Clubes de Lectura (Demo)');
    hideLoading();
}

async function loadMyClubs() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    displayClubs([mockClubs[0]], '👤 Mis Clubes de Lectura (Demo)');
    hideLoading();
}

// Users Functions (Demo versions)
async function loadAllUsers() {
    showLoading();
    await new Promise(resolve => setTimeout(resolve, 500));
    displayUsers(mockUsers, '👥 Todos los Usuarios (Demo)');
    hideLoading();
}

// Display Functions (same as original)
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
function loadCommunityOptions() {
    const select = document.getElementById('post-community');
    select.innerHTML = '<option value="">Post General (sin comunidad)</option>';
    mockCommunities.forEach(community => {
        select.innerHTML += `<option value="${community.id}">${community.name}</option>`;
    });
}

function loadPostComments(postId) {
    const commentsContainer = document.getElementById(`comments-${postId}`);

    if (commentsContainer.style.display === 'block') {
        commentsContainer.style.display = 'none';
        return;
    }

    const comments = mockComments[postId] || [];

    if (comments.length > 0) {
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
}

// Utility Functions (same as original)
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
    document.getElementById('users-list').innerHTML = '';
}

// Action Functions (Demo versions)
function deletePost(postId) {
    if (confirm('¿Estás seguro de que quieres eliminar este post?')) {
        const index = mockPosts.findIndex(post => post.id === postId);
        if (index > -1) {
            mockPosts.splice(index, 1);
            loadAllPosts();
            showSuccess('Post eliminado exitosamente (Demo)');
        }
    }
}

function joinCommunity(communityId) {
    showSuccess('Te has unido a la comunidad (Demo)');
}

function joinClub(clubId) {
    showSuccess('Te has unido al club de lectura (Demo)');
}

function followUser(userId) {
    showSuccess('Ahora sigues a este usuario (Demo)');
}

function loadCommunityPosts(communityId) {
    const communityPosts = mockPosts.filter(post => post.community && post.community.id === communityId);
    displayPosts(communityPosts, `📝 Posts de la Comunidad (Demo)`);
    // Switch to posts tab
    document.querySelector('[data-tab="posts"]').click();
}

function loadUserPosts(userId) {
    const userPosts = mockPosts.filter(post => post.user.id === userId);
    const user = mockUsers.find(u => u.id === userId);
    displayPosts(userPosts, `📝 Posts de ${user.name} (Demo)`);
    // Switch to posts tab
    document.querySelector('[data-tab="posts"]').click();
}

function viewBookDetails(bookId) {
    showSuccess(`Mostrando detalles del libro ${bookId} (Demo)`);
} 