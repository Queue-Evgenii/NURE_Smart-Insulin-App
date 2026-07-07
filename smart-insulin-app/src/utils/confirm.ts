import { alertController } from '@ionic/vue';

export interface ConfirmOptions {
  header: string;
  message: string;
  confirmText: string;
  cancelText: string;
  /** Style the confirm button as destructive (red). Defaults to true. */
  destructive?: boolean;
}

/** Show a confirmation dialog. Resolves to true only if the user confirms. */
export async function confirmAction(opts: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    alertController
      .create({
        header: opts.header,
        message: opts.message,
        buttons: [
          {
            text: opts.cancelText,
            role: 'cancel',
            handler: () => resolve(false),
          },
          {
            text: opts.confirmText,
            role: 'destructive',
            cssClass: (opts.destructive ?? true) ? 'alert-button-destructive' : undefined,
            handler: () => resolve(true),
          },
        ],
      })
      .then((alert) => {
        alert.onDidDismiss().then((ev) => {
          // Dismissed by backdrop / hardware back without pressing a button
          if (ev.role !== 'destructive' && ev.role !== 'cancel') resolve(false);
        });
        alert.present();
      });
  });
}
