import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, AppBar, Avatar, Badge, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, TablePagination, Snackbar, Toolbar, Typography, useMediaQuery, useTheme, } from '@mui/material'
import DashboardIcon from '@mui/icons-material/DashboardOutlined'
import ContactIcon from '@mui/icons-material/ContactsOutlined'
import LogoutIcon from '@mui/icons-material/LogoutOutlined'
import MenuIcon from '@mui/icons-material/Menu'
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined'
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined'
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined'
import CloseIcon from '@mui/icons-material/Close'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import { NavLink, Outlet } from 'react-router-dom'
import logo from '../images/dreamsol-logo.png'
import { approveExcelDownloadRequest, deleteNotification, downloadNotificationAttachment, getNotifications, markNotificationRead, readAccessProfile, rejectExcelDownloadRequest, type AppNotification, } from '../pages/app/components/contact-master/apis'
const width = 250
const collapsedWidth = 72
const menus = [
  [
    '/dashboard',
    'Dashboard',
    <DashboardIcon />,
  ],
  [
    '/contacts',
    'Contact',
    <ContactIcon />,
  ],
] as const
const isNotificationRead = (notification: AppNotification) => Boolean(notification.isRead ?? notification.read)
const toastMessage = (notification?: AppNotification | null) => {
  if (!notification)
    return ''
  if (notification.title?.toLowerCase().includes('excel download approval'))
    return 'Excel download approval request received.'
  const message = String(notification.message || '').trim()
  return message.length > 120 ? `${message.slice(0, 117)}...` : message
}
function DashboardLayout() {
  const theme = useTheme()
  const desktop = useMediaQuery(theme.breakpoints.up('md'))
  // const navigate = useNavigate()
  const [open, setOpen] = useState(true)
  const [notificationAnchor, setNotificationAnchor] = useState<HTMLElement | null>(null)
  const [notifications, setNotifications] = useState<AppNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(() => {
    const saved = Number(localStorage.getItem('notificationUnreadCount') ?? 0)
    return Number.isFinite(saved) ? saved : 0
  })
  const [notificationPage, setNotificationPage] = useState(0)
  const [notificationTotalElements, setNotificationTotalElements] = useState(0)
  const [notificationSize, setNotificationSize] = useState(5)
  const notificationInitialLoaded = useRef(false)
  const [selectedNotification, setSelectedNotification] = useState<AppNotification | null>(null)
  const [approvalWorking, setApprovalWorking] = useState(false)
  const [approvalMessage, setApprovalMessage] = useState('')
  const [approvalError, setApprovalError] = useState('')
  const access = readAccessProfile()
  const isAdmin = access.adminAccess
  const displayRole = access.displayRole
  const visibleMenus = access.dashboardAccess ? menus : menus.filter(([path]) => path !== '/dashboard')
  const openNotificationDetails = async (notification: AppNotification) => {
    const activeElement = document.activeElement
    if (activeElement instanceof HTMLElement) {
      activeElement.blur()
    }
    setNotificationToast((current) => current?.id === notification.id ? null : current)
    let selected = notification
    if (!isNotificationRead(notification)) {
      try {
        const nextUnreadCount = await markNotificationRead(notification.id)
        selected = {
          ...notification,
          isRead: true,
          read: true,
        }
        setNotifications((current) => current.map((item) => item.id === notification.id
          ? { ...item, isRead: true, read: true }
          : item))
        setUnreadCount(nextUnreadCount)
        localStorage.setItem('notificationUnreadCount', String(nextUnreadCount))
      }
      catch {
      }
    }
    window.requestAnimationFrame(() => {
      setSelectedNotification(selected)
    })
  }
  const [notificationToast, setNotificationToast] = useState<AppNotification | null>(null)
  const latestNotificationId = useRef<number | null>(null)
  const loadNotifications = useCallback(async (showToast = false, page = 0, size = notificationSize) => {
    try {
      const data = await getNotifications(page, size)
      const latest = data.items[0]
      if (showToast &&
        latest &&
        latest.id !== latestNotificationId.current) {
        setNotificationToast(latest)
      }
      if (latest) {
        latestNotificationId.current = latest.id
      }
      setNotifications(data.items)
      setUnreadCount(data.unreadCount)
      localStorage.setItem('notificationUnreadCount', String(data.unreadCount))
      setNotificationPage(data.page)
      setNotificationTotalElements(data.totalElements)
    }
    catch {
    }
  }, [notificationSize])
  useEffect(() => {
    // Old + new notifications must be available immediately after page load.
    // Guard prevents duplicate initial GET in React development mode.
    if (notificationInitialLoaded.current) {
      return
    }
    notificationInitialLoaded.current = true
    void loadNotifications(false, 0, notificationSize)
  }, [loadNotifications, notificationSize])
  useEffect(() => {
    const refresh = () => {
      void loadNotifications(true, 0, notificationSize)
    }
    window.addEventListener('notification-updated', refresh)
    return () => {
      window.removeEventListener('notification-updated', refresh)
    }
  }, [loadNotifications, notificationSize])
  const handleMarkRead = async (notification: AppNotification) => {
    if (isNotificationRead(notification)) {
      return
    }
    const nextUnreadCount = await markNotificationRead(notification.id)
    setNotifications((current) => current.map((item) => item.id === notification.id
      ? { ...item, isRead: true, read: true }
      : item))
    setSelectedNotification((current) => current?.id === notification.id
      ? { ...current, isRead: true, read: true }
      : current)
    setNotificationToast((current) => current?.id === notification.id ? null : current)
    setUnreadCount(nextUnreadCount)
    localStorage.setItem('notificationUnreadCount', String(nextUnreadCount))
  }
  const handleDeleteNotification = async (notification: AppNotification) => {
    const nextUnreadCount = await deleteNotification(notification.id)
    setNotifications((current) => current.filter((item) => item.id !== notification.id))
    setNotificationTotalElements((current) => Math.max(0, current - 1))
    setUnreadCount(nextUnreadCount)
    localStorage.setItem('notificationUnreadCount', String(nextUnreadCount))
    setSelectedNotification(null)
    setNotificationToast((current) => current?.id === notification.id ? null : current)
  }
  const handleApprovalDecision = async (decision: 'approve' | 'reject') => {
    const requestId = selectedNotification?.actionRequestId
    if (!requestId || !isAdmin)
      return
    setApprovalWorking(true)
    setApprovalMessage('')
    setApprovalError('')
    try {
      const message = decision === 'approve'
        ? await approveExcelDownloadRequest(requestId)
        : await rejectExcelDownloadRequest(requestId)
      setApprovalMessage(message)
      setSelectedNotification((current) => current
        ? { ...current, actionStatus: decision === 'approve' ? 'APPROVED' : 'REJECTED', read: true, isRead: true }
        : current)
      await loadNotifications(false, 0, notificationSize)
    }
    catch (error) {
      setApprovalError(error instanceof Error ? error.message : 'Unable to process approval request')
    }
    finally {
      setApprovalWorking(false)
    }
  }
  const handleLogout = () => {
    ['accessToken', 'tokenType', 'roles', 'accessProfile', 'isLoggedIn', 'loggedInUser', 'notificationUnreadCount']
      .forEach((key) => localStorage.removeItem(key))
    sessionStorage.clear()
    window.location.replace('/login')
  }
  const sidebar = (<Box sx={{
    height: '100vh',
    position: 'relative',
    px: desktop && !open
      ? 1
      : 2,
    py: 2,
    boxSizing: 'border-box',
    overflow: 'hidden',
  }}>
    <Box sx={{
      width: '100%',
      minHeight: 54,
      display: 'flex',
      alignItems: 'center',
      justifyContent: desktop && !open
        ? 'center'
        : 'flex-start',
      mb: 2,
    }}>
      <Box component='img' src={logo} alt='DreamSol' sx={{
        width: desktop && !open
          ? 42
          : 170,
        maxWidth: '100%',
        height: desktop && !open
          ? 42
          : 'auto',
        objectFit: 'contain',
      }} />
    </Box>
    <List sx={{
      p: 0,
    }}>
      {visibleMenus.map(([path, name, icon,]) => (<ListItemButton key={path} component={NavLink} to={path} title={desktop &&
        !open
        ? name
        : undefined} onClick={(event) => {
          event.currentTarget.blur()
          if (!desktop) {
            setOpen(false)
          }
        }} sx={{
          mb: 1,
          minHeight: 48,
          px: desktop &&
            !open
            ? 1
            : 1.5,
          borderRadius: 2,
          color: '#475569',
          justifyContent: desktop &&
            !open
            ? 'center'
            : 'flex-start',
          '&.active': {
            bgcolor: '#0f766e',
            color: 'white',
          },
          '&.active:hover': {
            bgcolor: '#0f766e',
            color: 'white',
          },
          '&:hover': {
            bgcolor: '#f0fdfa',
            color: '#0f766e',
          },
        }}>
        <ListItemIcon sx={{
          minWidth: desktop &&
            !open
            ? 0
            : 40,
          mr: desktop &&
            !open
            ? 0
            : 0.5,
          justifyContent: 'center',
          color: 'inherit',
        }}>
          {icon}
        </ListItemIcon>
        {(!desktop ||
          open) && (<ListItemText primary={name} />)}
      </ListItemButton>))}
    </List>
    <ListItemButton onClick={handleLogout} title={desktop && !open
      ? 'Logout'
      : undefined} sx={{
        position: 'absolute',
        bottom: 5,
        left: desktop && !open
          ? 8
          : 16,
        right: desktop && !open
          ? 8
          : 16,
        minHeight: 48,
        px: desktop && !open
          ? 1
          : 1.5,
        borderRadius: 2,
        color: '#475569',
        justifyContent: desktop && !open
          ? 'center'
          : 'flex-start',
        '&:hover': {
          bgcolor: '#f0fdfa',
          color: '#0f766e',
        },
      }}>
      <ListItemIcon sx={{
        minWidth: desktop && !open
          ? 0
          : 40,
        mr: desktop && !open
          ? 0
          : 0.5,
        justifyContent: 'center',
        color: 'inherit',
      }}>
        <LogoutIcon />
      </ListItemIcon>
      {(!desktop ||
        open) && (<ListItemText primary='Logout' />)}
    </ListItemButton>
  </Box>)
  return (<Box sx={{
    display: 'flex',
    height: '100vh',
    overflow: 'hidden',
    bgcolor: '#f0fdfa',
  }}>
    <Drawer variant={desktop
      ? 'permanent'
      : 'temporary'} open={desktop
        ? true
        : open} onClose={() => setOpen(false)} sx={{
          width: desktop
            ? open
              ? width
              : collapsedWidth
            : 0,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: desktop
              ? open
                ? width
                : collapsedWidth
              : width,
            height: '100vh',
            boxSizing: 'border-box',
            bgcolor: 'white',
            borderRight: '1px solid #ccfbf1',
            overflowX: 'hidden',
            transition: 'width 0.2s ease',
          },
        }}>
      {sidebar}
    </Drawer>
    <Box sx={{
      flex: 1,
      minWidth: 0,
      height: '100vh',
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
    }}>
      <AppBar position='static' elevation={0} sx={{
        bgcolor: '#0f766e',
        borderBottom: '1px solid #115e59',
        flexShrink: 0,
      }}>
        <Toolbar>
          <IconButton type='button' onClick={(event) => {
            event.currentTarget.blur()
            setOpen((prev) => !prev)
          }} sx={{
            color: 'white',
            mr: 1,
          }}>
            <MenuIcon />
          </IconButton>
          <Typography variant='h6' sx={{
            flex: 1,
            fontWeight: 600,
          }}>
            Contact Management System
          </Typography>
          <IconButton type='button' title='Notifications' onClick={(event) => {
            const anchor = event.currentTarget
            anchor.blur()
            setNotificationAnchor(anchor)
          }} sx={{ color: 'white', mr: 1 }}>
            <Badge badgeContent={unreadCount > 0 ? String(unreadCount) : 0} color='error'>
              <NotificationsNoneOutlinedIcon />
            </Badge>
          </IconButton>
          <Menu anchorEl={notificationAnchor} open={Boolean(notificationAnchor)} onClose={() => { }} slotProps={{
            paper: {
              sx: {
                width: 420,
                maxWidth: 'calc(100vw - 24px)',
                height: 430,
                maxHeight: 430,
                overflow: 'hidden',
                '& .MuiMenu-list': {
                  p: 0,
                  height: '100%',
                  maxHeight: '100%',
                  overflow: 'hidden !important',
                },
              },
            },
          }}>
            <Box sx={{
              height: '100%',
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}>
              <Box sx={{
                px: 2,
                py: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                bgcolor: '#0f766e',
                flexShrink: 0,
              }}>
                <Typography sx={{
                  fontWeight: 700,
                  flex: 1,
                  color: '#fff',
                }}>
                  Notifications
                </Typography>
                <IconButton size='small' title='Close' onClick={(event) => {
                  event.currentTarget.blur()
                  setNotificationAnchor(null)
                }} sx={{
                  color: '#f44336',
                  bgcolor: '#fff',
                  width: 28,
                  height: 28,
                  '&:hover': {
                    bgcolor: '#f5f5f5',
                  },
                }}>
                  <CloseIcon fontSize='small' />
                </IconButton>
              </Box>
              <Divider sx={{ borderColor: '#0f766e', flexShrink: 0 }} />
              <Box sx={{
                flex: '1 1 0',
                minHeight: 0,
                overflowY: 'auto',
                overflowX: 'hidden',
                overscrollBehavior: 'contain',
              }}>
                {notifications.length === 0 ? (<Box sx={{ px: 2, py: 3, textAlign: 'center', color: 'text.secondary' }}>
                  No notifications
                </Box>) : notifications.map((notification) => (<MenuItem key={notification.id} onClick={() => {
                  openNotificationDetails(notification)
                }} sx={{
                  alignItems: 'flex-start',
                  gap: 1,
                  py: 1.25,
                  pr: 1,
                  whiteSpace: 'normal',
                  bgcolor: isNotificationRead(notification) ? 'transparent' : 'action.hover',
                }}>
                  {!isNotificationRead(notification) && (<Box sx={{
                    width: 9,
                    height: 9,
                    borderRadius: '50%',
                    mt: 0.8,
                    flexShrink: 0,
                    bgcolor: notification.type === 'ERROR' ? 'error.main' : 'primary.main',
                  }} />)}
                  <Box sx={{ minWidth: 0, flex: 1, pl: isNotificationRead(notification) ? 2.1 : 0 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography sx={{
                        minWidth: 0,
                        flex: 1,
                        fontSize: 14,
                        fontWeight: isNotificationRead(notification) ? 400 : 700,
                        color: isNotificationRead(notification) ? 'text.secondary' : 'text.primary',
                      }} noWrap>
                        {notification.title}
                      </Typography>
                      <Typography component='span' sx={{
                        fontSize: 11,
                        fontWeight: 700,
                        whiteSpace: 'nowrap',
                        color: isNotificationRead(notification) ? 'text.secondary' : 'error.main',
                      }}>
                        {isNotificationRead(notification) ? '✓ Read' : 'Unread'}
                      </Typography>
                    </Box>
                    <Typography sx={{
                      fontSize: 12.5,
                      color: isNotificationRead(notification) ? 'text.disabled' : 'text.secondary',
                    }} noWrap>
                      {notification.message}
                    </Typography>
                  </Box>
                </MenuItem>))}
              </Box>
              {notificationTotalElements > 0 && (<Box sx={{
                flexShrink: 0,
                position: 'sticky',
                bottom: 0,
                zIndex: 5,
                bgcolor: '#0f766e',
                boxShadow: '0 -2px 8px rgba(0,0,0,0.12)',
              }}>
                <Divider />
                <TablePagination component='div' count={notificationTotalElements} page={notificationPage} rowsPerPage={notificationSize} rowsPerPageOptions={[5, 10, 15, 20, 25, 50, 100]} showFirstButton showLastButton onPageChange={(_, page) => void loadNotifications(false, page, notificationSize)} onRowsPerPageChange={(event) => {
                  const size = Number(event.target.value)
                  setNotificationSize(size)
                  setNotificationPage(0)
                  void loadNotifications(false, 0, size)
                }} labelRowsPerPage='Rows:' sx={{
                  width: '100%',
                  overflow: 'hidden',
                  bgcolor: '#0f766e',
                  color: '#fff',
                  '& .MuiTablePagination-toolbar': {
                    minHeight: 52,
                    px: 1,
                    gap: 0.5,
                    flexWrap: 'nowrap',
                  },
                  '& .MuiTablePagination-spacer': { display: 'none' },
                  '& .MuiTablePagination-selectLabel': {
                    m: 0,
                    fontSize: 12,
                    whiteSpace: 'nowrap',
                  },
                  '& .MuiTablePagination-select': {
                    fontSize: 12,
                    pr: '22px !important',
                  },
                  '& .MuiTablePagination-displayedRows': {
                    m: 0,
                    ml: 'auto',
                    fontSize: 12,
                    whiteSpace: 'nowrap',
                  },
                  '& .MuiTablePagination-actions': {
                    ml: 0.5,
                    display: 'flex',
                    gap: 0,
                    flexShrink: 0,
                  },
                  '& .MuiTablePagination-selectIcon': { color: '#fff' },
                  '& .MuiIconButton-root': {
                    color: '#fff',
                    p: 0.5,
                  },
                }} />
              </Box>)}
            </Box>
          </Menu>
          <Avatar sx={{
            width: 38,
            height: 38,
            bgcolor: '#ccfbf1',
            color: '#0f766e',
            border: '2px solid #5eead4',
          }}>
            <AdminPanelSettingsOutlinedIcon fontSize='small' />
          </Avatar>
          <Typography variant='body2' sx={{
            ml: 1,
            display: {
              xs: 'none',
              sm: 'block',
            },
            fontWeight: 600,
            color: 'white',
          }}>
            {displayRole}
          </Typography>
          <IconButton type='button' title='Logout' onClick={handleLogout} sx={{
            color: 'white',
            ml: 1,
            '&:hover': {
              bgcolor: 'rgba(255,255,255,0.12)',
            },
          }}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Box component='main' sx={{
        flex: 1,
        minHeight: 0,
        overflow: 'auto',
        p: {
          xs: 2,
          sm: 2.5,
          md: 3,
        },
      }}>
        <Outlet />
      </Box>
      <Box component='footer' sx={{
        flexShrink: 0,
        py: 1.2,
        textAlign: 'center',
        bgcolor: 'white',
        borderTop: '1px solid #ccfbf1',
      }}>
        <Typography variant='caption' sx={{
          color: '#64748b',
        }}>
          © 2026 DreamSol
        </Typography>
      </Box>
      <Dialog open={Boolean(selectedNotification)} onClose={() => {
      }} fullWidth maxWidth='sm'>
        <DialogTitle sx={{ bgcolor: '#0f9187', color: 'white', fontWeight: 700, display: 'flex', alignItems: 'center' }}>
          <Typography sx={{ flex: 1, fontWeight: 700 }}>
            {selectedNotification?.actionType === 'EXCEL_DOWNLOAD_APPROVAL' ? 'Download Approval' : 'Mail Details'}
          </Typography>
          <IconButton size='small' title='Close' onClick={() => {
            setSelectedNotification(null)
            setApprovalMessage('')
            setApprovalError('')
          }} sx={{
            color: '#d32f2f',
            bgcolor: '#fff',
            border: '1px solid #d32f2f',
            '&:hover': { bgcolor: '#ffebee' },
          }}>
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          {selectedNotification?.actionType === 'EXCEL_DOWNLOAD_APPROVAL' ? (<Box>
            <Typography sx={{ textAlign: 'center', color: '#0f766e', fontWeight: 800, fontSize: 19, mb: 1 }}>
              Excel Download Approval
            </Typography>
            {approvalMessage && <Alert severity='success' sx={{ mb: 2 }}>{approvalMessage}</Alert>}
            {approvalError && <Alert severity='error' sx={{ mb: 2 }}>{approvalError}</Alert>}
            <Box sx={{ border: '1px solid #99d5cf', borderRadius: 1, overflow: 'hidden', mb: 2 }}>
              <Box sx={{ bgcolor: '#0f766e', color: '#fff', px: 1.5, py: 1, fontWeight: 700 }}>
                Approval Request
              </Box>
              <Typography component='pre' sx={{
                m: 0,
                p: 2,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontFamily: 'inherit',
                fontSize: 14,
                lineHeight: 1.7,
                color: '#334155',
              }}>
                {selectedNotification.message}
              </Typography>
            </Box>
            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 1, fontSize: 14 }}>
              <b>Status:</b>
              <span>{selectedNotification.actionStatus || 'PENDING'}</span>
              <b>Requested At:</b>
              <span>{selectedNotification.createdAt ? new Date(selectedNotification.createdAt).toLocaleString() : '-'}</span>
            </Box>
          </Box>) : (<>
            <Typography sx={{ textAlign: 'center', color: '#244c7a', fontWeight: 700, fontSize: 18 }}>DreamSol</Typography>
            <Typography sx={{ textAlign: 'center', color: '#244c7a', fontWeight: 700, mb: 2 }}>
              {selectedNotification?.title}
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: '70px 1fr', gap: 0.5, mb: 2, fontSize: 14 }}>
              <b>From:</b><span>DreamSol System</span>
              <b>To:</b><span>{selectedNotification?.audienceEmail || selectedNotification?.audienceRole || 'User'}</span>
              <b>Subject:</b><span>{selectedNotification?.title}</span>
            </Box>
            <Typography sx={{ mb: 1 }}>Dear User,</Typography>
            <Typography sx={{ color: 'text.secondary', mb: 2 }}>
              {selectedNotification?.message}
            </Typography>
            {(selectedNotification?.totalCount != null || selectedNotification?.savedCount != null || selectedNotification?.updatedCount != null) && (<Box sx={{ border: '1px solid #aab4c3', mb: 2 }}>
              <Box sx={{ bgcolor: '#0f766e', color: 'white', px: 1.5, py: 0.8, fontWeight: 700 }}>Contact Summary</Box>
              {[
                ['Total Records', selectedNotification?.totalCount ?? 0, 'New Saved Records', selectedNotification?.savedCount ?? 0],
                ['Updated Records', selectedNotification?.updatedCount ?? 0, 'Duplicate Records', selectedNotification?.duplicateCount ?? 0],
                ['Invalid Records', selectedNotification?.invalidCount ?? 0, 'Successful Records', (selectedNotification?.savedCount ?? 0) + (selectedNotification?.updatedCount ?? 0)],
              ].map((row, index) => (<Box key={index} sx={{ display: 'grid', gridTemplateColumns: '1.2fr .8fr 1.2fr .8fr', borderTop: '1px solid #aab4c3' }}>
                {row.map((value, columnIndex) => (<Box key={columnIndex} sx={{ p: 1, borderRight: columnIndex < 3 ? '1px solid #aab4c3' : 0, fontWeight: columnIndex % 2 ? 700 : 400 }}>
                  {value}
                </Box>))}
              </Box>))}
            </Box>)}
            {selectedNotification?.attachmentName && (<Box sx={{ border: '1px solid #aab4c3', borderRadius: 1, p: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
              <Typography sx={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {selectedNotification.attachmentName}
              </Typography>
              <Button size='small' startIcon={<DownloadOutlinedIcon />} onClick={() => downloadNotificationAttachment(selectedNotification)}>
                Download
              </Button>
            </Box>)}
            {selectedNotification?.createdAt && (<Typography sx={{ mt: 2, fontSize: 12, color: 'text.secondary' }}>
              {new Date(selectedNotification.createdAt).toLocaleString()}
            </Typography>)}
          </>)}
        </DialogContent>
        <DialogActions sx={{ px: 2, py: 1.5, gap: 1 }}>
          {selectedNotification?.actionType === 'EXCEL_DOWNLOAD_APPROVAL' &&
            isAdmin &&
            String(selectedNotification.actionStatus || 'PENDING').toUpperCase() === 'PENDING' && (<>
              <Button variant='contained' disabled={approvalWorking} onClick={() => void handleApprovalDecision('approve')} sx={{ bgcolor: '#0f9187', '&:hover': { bgcolor: '#0f766e' } }}>
                {approvalWorking ? 'Processing...' : 'Approve'}
              </Button>
              <Button variant='outlined' color='error' disabled={approvalWorking} onClick={() => void handleApprovalDecision('reject')}>
                Reject
              </Button>
            </>)}
          {selectedNotification && !isNotificationRead(selectedNotification) && (<Button onClick={async () => {
            await handleMarkRead(selectedNotification)
            setSelectedNotification({ ...selectedNotification, isRead: true, read: true })
          }}>
            Mark as read
          </Button>)}
          {selectedNotification && (<Button color='error' startIcon={<DeleteOutlineOutlinedIcon />} onClick={() => handleDeleteNotification(selectedNotification)}>
            Delete
          </Button>)}
        </DialogActions>
      </Dialog>
      <Snackbar open={Boolean(notificationToast)} autoHideDuration={null} onClose={() => {
      }} anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
        <Alert severity='success' onClick={() => {
          if (notificationToast) {
            void openNotificationDetails(notificationToast)
          }
        }} sx={{ width: { xs: 'calc(100vw - 32px)', sm: 520 }, maxWidth: 520, alignItems: 'flex-start', cursor: 'pointer' }} action={notificationToast ? (<Box sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
          {!isNotificationRead(notificationToast) && (<Button size='small' onClick={async (event) => {
            event.stopPropagation()
            await handleMarkRead(notificationToast)
            setNotificationToast(null)
          }} sx={{ minWidth: 0, px: 1, whiteSpace: 'nowrap' }}>
            Mark as read
          </Button>)}
          <IconButton size='small' title='Delete notification' onClick={async (event) => {
            event.stopPropagation()
            await handleDeleteNotification(notificationToast)
            setNotificationToast(null)
          }}>
            <DeleteOutlineOutlinedIcon fontSize='small' />
          </IconButton>
          <IconButton size='small' title='Close notification' onClick={(event) => {
            event.stopPropagation()
            setNotificationToast(null)
          }} sx={{ color: '#d32f2f' }}>
            <CloseIcon fontSize='small' />
          </IconButton>
        </Box>) : undefined}>
          <Typography sx={{ fontWeight: 700 }}>{notificationToast?.title}</Typography>
          <Typography sx={{ fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{toastMessage(notificationToast)}</Typography>
          <Typography sx={{ mt: 0.5, fontSize: 12, fontWeight: 700, color: 'primary.main' }}>Click to show details</Typography>
          {notificationToast?.attachmentName && (<Button size='small' startIcon={<DownloadOutlinedIcon />} sx={{ mt: 0.5 }} onClick={(event) => { event.stopPropagation(); downloadNotificationAttachment(notificationToast); }}>
            {notificationToast.attachmentName}
          </Button>)}
        </Alert>
      </Snackbar>
    </Box>
  </Box>)
}
export default DashboardLayout
