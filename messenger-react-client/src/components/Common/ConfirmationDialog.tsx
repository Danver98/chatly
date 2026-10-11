import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';

export interface IConfirmDialogOptions {
  open: boolean;
  title?: string;
  message?: string;
  confirmText?: string;
  closeText?: string;
  onClose?: () => void;
  onConfirm?: () => void;
}

/**
 * Common purpose confirmation dialog 
 */
export default function ConfirmationDialog({ options }: { options: IConfirmDialogOptions }) {
  return (
    <Dialog open={options.open} onClose={options.onClose}>
      <DialogTitle>{options.title}</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {options.message}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={options.onClose} color="inherit">
          {options.closeText ?? 'Cancel'}
        </Button>
        <Button onClick={options.onConfirm} color="error" autoFocus>
          {options.confirmText ?? 'Confirm'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}